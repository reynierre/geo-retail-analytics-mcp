# Arquitectura del Sistema

## Visión General

El sistema de Retail Analytics MCP permite consultar datos de ventas mediante lenguaje natural, utilizando el Model Context Protocol (MCP) para conectar un LLM con una base de datos analítica.

## Diagrama de Arquitectura

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                   USUARIO                                        │
│                        "¿Cuánto vendió el local 001?"                           │
└─────────────────────────────────────┬───────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            ANGULAR 21 FRONTEND                                   │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐                  │
│  │  Chat Component │  │  Chat Service   │  │  Auth Service   │                  │
│  │    (Signals)    │  │  (SSE Stream)   │  │     (JWT)       │                  │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘                  │
│  Tailwind CSS • Zoneless • Standalone Components                                 │
└─────────────────────────────────────┬───────────────────────────────────────────┘
                                      │ HTTP/SSE (Port 4200 → 8080)
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         SPRING BOOT BACKEND (API Gateway)                        │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐                  │
│  │ ChatController  │  │  ChatService    │  │  McpClient      │                  │
│  │   (SSE Stream)  │  │ (Orchestrator)  │  │  (MCP Protocol) │                  │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘                  │
│  Spring AI • WebFlux • JWT Security                                              │
└───────────┬─────────────────────────────────────────────────┬───────────────────┘
            │                                                 │
            │ HTTP (Port 8080 → 11434)                       │ MCP (Port 8080 → 8081)
            ▼                                                 ▼
┌───────────────────────────┐                 ┌───────────────────────────────────┐
│   LLM (Ollama/Claude)     │                 │        MCP SERVER (Spring AI)     │
│  ┌─────────────────────┐  │                 │  ┌─────────────────────────────┐  │
│  │  llama3.1:8b (Dev)  │  │                 │  │   RetailAnalyticsTools      │  │
│  │  Claude Sonnet (Pr) │  │                 │  │   • get_store_sales         │  │
│  └─────────────────────┘  │                 │  │   • get_stores_ranking      │  │
│  Tool Calling • Streaming │                 │  │   • get_top_products        │  │
└───────────────────────────┘                 │  │   • get_hourly_traffic      │  │
                                              │  │   • compare_periods         │  │
                                              │  └─────────────────────────────┘  │
                                              │  @Tool Annotation • HTTP Transport │
                                              └───────────────────┬───────────────┘
                                                                  │
                                                                  │ JDBC (Port 8081 → 8123)
                                                                  ▼
                                              ┌───────────────────────────────────┐
                                              │           CLICKHOUSE              │
                                              │  ┌─────────────────────────────┐  │
                                              │  │  fact_tickets (500K+)       │  │
                                              │  │  fact_ticket_items (2.5M+)  │  │
                                              │  │  dim_stores (100)           │  │
                                              │  │  dim_products (1000)        │  │
                                              │  │  mv_daily_sales             │  │
                                              │  │  mv_hourly_traffic          │  │
                                              │  └─────────────────────────────┘  │
                                              │  Columnar Storage • MergeTree     │
                                              └───────────────────────────────────┘
```

## Flujo de Datos

### 1. Pregunta del Usuario

```
Usuario → "¿Cuánto vendió el local 001 en enero?"
         → Angular ChatComponent
         → ChatService.streamChat()
         → POST /api/chat/stream (SSE)
```

### 2. Procesamiento en Backend

```
Backend ChatController
  → ChatService.processMessage()
  → ChatClient.prompt().tools(mcpTools).stream()
  → Ollama/Claude API
```

### 3. Tool Calling (LLM → MCP)

```
LLM identifica necesidad de datos
  → Tool call: get_store_sales(store_id="001", start="2024-01-01", end="2024-01-31")
  → McpClient envía request al MCP Server
  → MCP Server ejecuta query en ClickHouse
  → Resultado retorna al LLM
```

### 4. Generación de Respuesta

```
LLM genera respuesta con datos reales
  → Streaming tokens al Backend
  → SSE chunks al Frontend
  → Renderizado progresivo en UI
```

## Componentes

### Frontend (Angular 21)

**Responsabilidades:**
- UI del chat con streaming
- Autenticación de usuarios
- Historial de conversaciones

**Tecnologías clave:**
- Signals para estado reactivo
- Zoneless change detection
- fetch API con SSE para streaming
- Tailwind CSS para estilos

### Backend API (Spring Boot 3.4)

**Responsabilidades:**
- Gateway entre frontend y servicios
- Orquestación LLM + MCP
- Autenticación JWT
- Rate limiting

**Tecnologías clave:**
- Spring AI para integración LLM
- Spring WebFlux para streaming
- Spring Security para auth

### MCP Server (Spring AI MCP)

**Responsabilidades:**
- Exponer tools al LLM
- Ejecutar queries en ClickHouse
- Validar y formatear respuestas

**Tecnologías clave:**
- Spring AI MCP con @Tool annotation
- HTTP transport
- JDBC para ClickHouse

### ClickHouse

**Responsabilidades:**
- Almacenar datos transaccionales
- Procesar queries analíticas
- Pre-agregar métricas comunes

**Características clave:**
- Columnar storage (6-10x compresión)
- Vectorized execution
- Materialized Views para agregaciones

## Decisiones de Diseño

### ¿Por qué MCP en lugar de function calling directo?

1. **Separación de concerns**: El MCP Server encapsula la lógica de datos
2. **Reutilización**: Múltiples clientes pueden usar el mismo MCP Server
3. **Testabilidad**: Tools se pueden testear independientemente
4. **Evolución**: Agregar tools sin modificar el backend principal

### ¿Por qué ClickHouse en lugar de PostgreSQL?

1. **Velocidad**: 100x más rápido para agregaciones
2. **Compresión**: 6-10x menos storage
3. **Escala**: Millones de filas sin degradación
4. **Columnar**: Lee solo las columnas necesarias

### ¿Por qué Ollama para desarrollo?

1. **Costo**: 100% gratuito
2. **Privacidad**: Datos no salen de tu máquina
3. **Velocidad de iteración**: Sin límites de API
4. **Tool calling**: Llama 3.1 soporta tools nativamente

## Escalabilidad

### Horizontal

```
                    ┌─────────────┐
                    │   Nginx     │
                    │   (LB)      │
                    └──────┬──────┘
           ┌───────────────┼───────────────┐
           ▼               ▼               ▼
    ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
    │  Backend 1  │ │  Backend 2  │ │  Backend 3  │
    └──────┬──────┘ └──────┬──────┘ └──────┬──────┘
           └───────────────┼───────────────┘
                           ▼
                    ┌─────────────┐
                    │ MCP Server  │
                    │  (Shared)   │
                    └──────┬──────┘
                           ▼
                    ┌─────────────┐
                    │ ClickHouse  │
                    │  Cluster    │
                    └─────────────┘
```

### Vertical (Recursos recomendados)

| Volumen | CPU | RAM | Storage |
|---------|-----|-----|---------|
| < 10M tickets | 4 cores | 8 GB | 50 GB SSD |
| 10-50M tickets | 8 cores | 32 GB | 200 GB NVMe |
| 50-100M tickets | 16 cores | 64 GB | 500 GB NVMe |

## Seguridad

### Capas de Seguridad

1. **Frontend**: JWT en localStorage, HTTPS
2. **Backend**: Spring Security, rate limiting
3. **MCP Server**: Red interna, no expuesto públicamente
4. **ClickHouse**: Red interna, credentials en env vars

### Queries Seguras

```java
// ✅ BIEN: Queries parametrizadas
String sql = "SELECT * FROM tickets WHERE store_id = ?";
jdbcTemplate.query(sql, storeId);

// ❌ MAL: Concatenación de strings
String sql = "SELECT * FROM tickets WHERE store_id = '" + storeId + "'";
```
