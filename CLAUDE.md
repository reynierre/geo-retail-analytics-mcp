# 🏗️ Geo Retail Analytics MCP - Sistema de Consultas Conversacionales para Retail

## 📋 Resumen del Proyecto

Sistema que permite consultar datos de tiendas retail mediante lenguaje natural. Un usuario puede preguntar "¿Cuánto vendió el local 001 este mes?" y obtener una respuesta procesada por un LLM que consulta los datos reales.

### Arquitectura de Alto Nivel

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              USUARIO                                         │
│                    "¿Cuánto vendió el local 001?"                           │
└─────────────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         ANGULAR 21 FRONTEND                                  │
│  Chat UI con streaming, Signals, Zoneless, Tailwind CSS                     │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │ HTTP/SSE
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      SPRING BOOT BACKEND (Unificado)                         │
│                                                                              │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐              │
│  │  API Controller │  │  Chat Service   │  │  Analytics      │              │
│  │  (REST + SSE)   │  │  (LLM Client)   │  │  Tools (@Tool)  │              │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘              │
│           │                    │                    │                        │
│           └────────────────────┴────────────────────┘                        │
│                                │                                             │
│                    ┌───────────┴───────────┐                                 │
│                    ▼                       ▼                                 │
│           ┌─────────────────┐    ┌─────────────────┐                        │
│           │ ClickHouse Repo │    │ LLM Integration │                        │
│           └─────────────────┘    └─────────────────┘                        │
└────────────────────┬─────────────────────┬──────────────────────────────────┘
                     │                     │
                     ▼                     ▼
          ┌─────────────────────┐  ┌─────────────────────┐
          │     CLICKHOUSE      │  │   OLLAMA / CLAUDE   │
          │   (Analytics DB)    │  │   (LLM Provider)    │
          └─────────────────────┘  └─────────────────────┘
```

**Arquitectura simplificada: Un solo backend que contiene todo.**

---

## 🛠️ Stack Tecnológico (100% Open Source para desarrollo)

| Capa | Tecnología | Versión | Licencia | Función |
|------|------------|---------|----------|---------|
| **Frontend** | Angular | 21.x | MIT | Chat UI con streaming |
| **Backend** | Spring Boot | 3.4.x | Apache 2.0 | API + LLM + Tools (todo junto) |
| **Build Tool** | Gradle | 8.11.x | Apache 2.0 | Build y gestión de dependencias |
| **LLM Tools** | Spring AI | 1.0.x | Apache 2.0 | Anotación @Tool para tools |
| **LLM (Dev/MVP)** | Grok (xAI) | Free Tier | Gratuito | LLM gratuito para desarrollo y MVP |
| **LLM (Prod)** | Ollama + Llama 3.1 | 8B/70B | Apache 2.0 | LLM local para datos de producción |
| **Analytics DB** | ClickHouse | 24.x | Apache 2.0 | Queries analíticas ultra-rápidas |
| **Contenedores** | Docker Compose | - | Apache 2.0 | Orquestación local |
| **Arquitectura** | DDD + Clean Code | - | - | Domain-Driven Design con 3 capas |

---

## 📁 Estructura del Proyecto

```
geo-retail-analytics-mcp/
├── CLAUDE.md                          # Este archivo - Contexto del proyecto
├── .claude/
│   ├── agents/                        # Subagentes especializados
│   │   ├── angular-expert.md          # Experto en Angular 21
│   │   ├── spring-boot-expert.md      # Experto en Spring Boot
│   │   ├── mcp-expert.md              # Experto en MCP/Tools
│   │   ├── clickhouse-expert.md       # Experto en ClickHouse
│   │   └── llm-integration-expert.md  # Experto en integración LLM
│   └── skills/                        # Skills del proyecto
│       ├── angular21/SKILL.md
│       ├── spring-ai-mcp/SKILL.md
│       ├── clickhouse/SKILL.md
│       ├── ollama-integration/SKILL.md
│       └── retail-domain/SKILL.md
├── frontend/                          # Angular 21 Chat UI
│   ├── src/app/
│   │   ├── chat/                      # Componentes del chat
│   │   ├── services/                  # ChatService, ApiService
│   │   └── shared/                    # Componentes compartidos
│   ├── angular.json
│   └── package.json
├── backend/                           # Spring Boot + DDD + Clean Code
│   ├── src/main/java/com/geocom/retail/
│   │   ├── RetailAnalyticsApp.java    # Main class
│   │   │
│   │   ├── application/               # ← CAPA APPLICATION (Use Cases)
│   │   │   ├── service/
│   │   │   │   └── ChatService.java           # Orquestación LLM
│   │   │   ├── usecase/
│   │   │   │   ├── GetStoreSalesUseCase.java
│   │   │   │   └── GetStoresRankingUseCase.java
│   │   │   └── dto/
│   │   │       ├── ChatRequest.java
│   │   │       └── ChatResponse.java
│   │   │
│   │   ├── domain/                    # ← CAPA DOMAIN (Entidades y Reglas)
│   │   │   ├── model/
│   │   │   │   ├── Store.java
│   │   │   │   ├── Ticket.java
│   │   │   │   └── SalesReport.java
│   │   │   ├── repository/            # Interfaces (ports)
│   │   │   │   └── SalesRepository.java
│   │   │   └── service/
│   │   │       └── SalesAnalyticsDomainService.java
│   │   │
│   │   └── infrastructure/            # ← CAPA INFRASTRUCTURE (Adapters)
│   │       ├── adapter/
│   │       │   ├── in/
│   │       │   │   └── web/
│   │       │   │       └── ChatController.java
│   │       │   └── out/
│   │       │       ├── persistence/
│   │       │       │   └── ClickHouseRepositoryImpl.java
│   │       │       └── llm/
│   │       │           ├── GrokLlmAdapter.java      # Dev/MVP
│   │       │           └── OllamaLlmAdapter.java    # Producción
│   │       ├── config/
│   │       │   ├── LlmConfig.java
│   │       │   └── ClickHouseConfig.java
│   │       └── tools/                 # ← Tools MCP aquí
│   │           └── RetailAnalyticsTools.java
│   │
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-dev.yml        # Usa Grok (gratuito)
│   │   └── application-prod.yml       # Usa Ollama (local)
│   └── build.gradle                   # Gradle build file
├── database/                          # Scripts ClickHouse
│   └── schema/
│       ├── 001_create_tables.sql      # DDL: tablas y vistas
│       └── 002_seed_data.sql          # Datos de prueba (500K tickets)
├── docker/
│   ├── docker-compose.dev.yml         # ClickHouse + Ollama
│   └── .env.example
└── docs/
    └── ARCHITECTURE.md
```

### ¿Por qué `database/`?

La carpeta `database/schema/` contiene scripts SQL que:
1. **Se versionan en Git** - Cambios al schema son trazables
2. **Se ejecutan automáticamente** - Docker los corre al iniciar ClickHouse
3. **Incluyen datos de prueba** - 500K tickets listos para desarrollo

---

## 🎯 Dominio de Negocio: Sale (Geocom POS)

### Estructura del Dominio

```
Sale (Aggregate Root)
├── companyId, storeId, posTerminalId, ticketNumber
├── saleDate, cashierId
├── amounts: subtotal, discount, tax, rounding, surcharge, total
├── fiscalDocument: typeId, series, number (CFE Uruguay)
├── customer: taxTypeId, taxNumber
│
├── SaleItem[] (lineas)
│   ├── productId, barcode, quantity
│   ├── grossUnitPrice, netUnitPrice, discountAmount
│   ├── subtotalAmount, taxAmount, taxCode, totalAmount
│   │
│   └── SaleItemDiscount[] (descuentos por linea)
│       ├── discountAmount, discountPercentage, appliedQuantity
│       ├── promotionId, promotionName, promotionType
│       └── financedDiscount, agreementCode
│
└── SalePayment[] (pagos)
    ├── paymentMethodId, currencyCode, exchangeRate
    ├── tenderedAmount, totalAmount, discountAmount, surchargeAmount
    ├── Card: brandId, maskedNumber, type, entryMode, authCode
    ├── Plan: paymentPlanId, installmentCount
    ├── VAT: vatDiscountApplied, taxLawCode
    ├── Check: number, bankCode, expirationDate
    └── Account: type, number, authCode
```

### Tablas ClickHouse

| Tabla | Descripcion | Source DTO |
|-------|-------------|------------|
| `fact_sales` | Ventas (aggregate root) | InterfaceVtaDTO |
| `fact_sale_items` | Lineas de venta | InterfaceVtaDetDTO |
| `fact_sale_item_discounts` | Descuentos por linea | InterfaceVtaDtosDTO |
| `fact_sale_payments` | Pagos | InterfaceVtaPagoDTO |
| `dim_companies` | Empresas | - |
| `dim_stores` | Sucursales | - |
| `dim_products` | Productos | - |
| `dim_payment_methods` | Metodos de pago | - |
| `dim_card_brands` | Marcas de tarjeta | - |
| `dim_promotions` | Promociones | - |

### Vistas Materializadas

| Vista | Descripcion |
|-------|-------------|
| `mv_daily_sales` | Resumen diario por tienda |
| `mv_hourly_sales` | Trafico por hora |
| `mv_payment_summary` | Resumen por metodo de pago |
| `mv_product_sales` | Ventas por producto |
| `mv_promotion_summary` | Resumen de promociones |
| `mv_card_brand_summary` | Resumen por marca de tarjeta |

### Tools Definidos

| Tool | Descripcion | Parametros |
|------|-------------|------------|
| `get_store_sales` | Ventas de un local en periodo | store_id, start_date, end_date |
| `get_stores_ranking` | Ranking de locales por ventas | start_date, end_date, limit |
| `get_top_products` | Productos mas vendidos | category?, start_date, end_date, limit |
| `get_hourly_traffic` | Tickets por hora del dia | store_id?, date |
| `compare_periods` | Comparativa periodo vs periodo | period1, period2 |
| `get_payment_summary` | Resumen por metodo de pago | store_id?, start_date, end_date |
| `get_card_brand_summary` | Ventas por marca de tarjeta | store_id?, start_date, end_date |

---

## ⚙️ Código de Referencia (Arquitectura DDD + Clean Code)

### Domain Layer - Entidad y Puerto

```java
// domain/model/SalesReport.java
public record SalesReport(
    String storeId,
    String storeName,
    long ticketCount,
    BigDecimal totalSales,
    BigDecimal averageTicket,
    LocalDate startDate,
    LocalDate endDate
) {}

// domain/repository/SalesRepository.java (Port)
public interface SalesRepository {
    SalesReport findSalesByStoreAndPeriod(String storeId, LocalDate start, LocalDate end);
    List<StoreRanking> findStoresRanking(LocalDate start, LocalDate end, int limit);
}
```

### Application Layer - Use Case

```java
// application/usecase/GetStoreSalesUseCase.java
@Service
@RequiredArgsConstructor
public class GetStoreSalesUseCase {

    private final SalesRepository salesRepository;

    public SalesReport execute(String storeId, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        return salesRepository.findSalesByStoreAndPeriod(storeId, startDate, endDate);
    }

    private void validateDateRange(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new InvalidDateRangeException("Start date must be before end date");
        }
    }
}
```

### Infrastructure Layer - Adapter (ClickHouse)

```java
// infrastructure/adapter/out/persistence/ClickHouseRepositoryImpl.java
@Repository
@RequiredArgsConstructor
public class ClickHouseRepositoryImpl implements SalesRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public SalesReport findSalesByStoreAndPeriod(String storeId, LocalDate start, LocalDate end) {
        String sql = """
            SELECT store_id, count() as tickets, sum(total_amount) as sales
            FROM fact_tickets
            WHERE store_id = ? AND ticket_date BETWEEN ? AND ?
            GROUP BY store_id
            """;
        return jdbcTemplate.queryForObject(sql, this::mapToSalesReport, storeId, start, end);
    }
}
```

### Infrastructure Layer - LLM Adapters

```java
// infrastructure/adapter/out/llm/GrokLlmAdapter.java (Dev/MVP - Gratuito)
@Component
@Profile("dev")
@RequiredArgsConstructor
public class GrokLlmAdapter implements LlmPort {

    private final OpenAiChatModel grokModel;  // Usa API compatible OpenAI

    @Override
    public Flux<String> streamResponse(String prompt, List<Tool> tools) {
        return ChatClient.builder(grokModel)
            .build()
            .prompt().user(prompt).tools(tools)
            .stream().content();
    }
}

// infrastructure/adapter/out/llm/OllamaLlmAdapter.java (Producción - Local)
@Component
@Profile("prod")
@RequiredArgsConstructor
public class OllamaLlmAdapter implements LlmPort {

    private final OllamaChatModel ollamaModel;

    @Override
    public Flux<String> streamResponse(String prompt, List<Tool> tools) {
        return ChatClient.builder(ollamaModel)
            .build()
            .prompt().user(prompt).tools(tools)
            .stream().content();
    }
}
```

### Infrastructure Layer - Tool con @Tool

```java
// infrastructure/tools/RetailAnalyticsTools.java
@Service
@RequiredArgsConstructor
public class RetailAnalyticsTools {

    private final GetStoreSalesUseCase getStoreSalesUseCase;
    private final GetStoresRankingUseCase getStoresRankingUseCase;

    @Tool(name = "get_store_sales",
          description = "Obtiene ventas totales de un local en un período")
    public SalesReport getStoreSales(
            @ToolParam(description = "ID del local, ej: '001'") String storeId,
            @ToolParam(description = "Fecha inicio YYYY-MM-DD") String startDate,
            @ToolParam(description = "Fecha fin YYYY-MM-DD") String endDate) {

        return getStoreSalesUseCase.execute(
            storeId,
            LocalDate.parse(startDate),
            LocalDate.parse(endDate)
        );
    }
}
```

### Infrastructure Layer - Controller (Adapter In)

```java
// infrastructure/adapter/in/web/ChatController.java
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@RequestBody @Valid ChatRequest request) {
        return chatService.streamChat(request.message())
            .map(chunk -> ServerSentEvent.builder(chunk).build());
    }
}
```

---

## 🚀 Comandos del Proyecto

```bash
# === INFRAESTRUCTURA ===

# Levantar ClickHouse (ejecuta scripts de database/ automáticamente)
docker compose -f docker/docker-compose.dev.yml up -d

# === BACKEND (Gradle) ===

cd backend

# Desarrollo con Grok (gratuito)
./gradlew bootRun --args='--spring.profiles.active=dev'

# Producción con Ollama (local)
./gradlew bootRun --args='--spring.profiles.active=prod'

# Build
./gradlew build

# Tests
./gradlew test

# === OLLAMA (Solo para producción) ===

# Instalar Ollama
curl -fsSL https://ollama.com/install.sh | sh

# Iniciar Ollama y descargar modelo
ollama serve
ollama pull llama3.1:8b

# === FRONTEND ===

cd frontend
npm install
ng serve

# === VERIFICAR ===

# ClickHouse
docker exec -it retail-clickhouse clickhouse-client -q "SELECT count() FROM retail_analytics.fact_tickets"

# Backend health
curl http://localhost:8080/actuator/health

# Chat test
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "¿Cuánto vendió el local 001 ayer?"}'
```

---

## 🔐 Seguridad

### ❌ NUNCA HACER
- Exponer API keys en frontend
- Concatenar strings en queries SQL
- Loggear datos sensibles
- Commits con credenciales

### ✅ SIEMPRE HACER
- Variables de entorno para secrets (.env)
- Queries parametrizadas
- Validación de inputs

---

## 📊 Performance Targets

| Métrica | Target |
|---------|--------|
| Query ClickHouse | < 500ms p95 |
| Latencia total (LLM + query) | < 3s p95 |
| Streaming first token | < 500ms |
| Frontend bundle | < 500KB gzipped |

---

## 🔄 Git Workflow

```bash
# Branches
main              # Producción
develop           # Integración
feature/*         # Nuevas features

# Commits (Conventional Commits)
feat(backend): add store sales tool
fix(frontend): resolve streaming disconnect
docs: update architecture
```

---

## 🤖 Uso de Agentes y Skills

### Subagentes Disponibles

```
> Use the angular-expert agent to implement the chat component
> Have the clickhouse-expert agent optimize this query
> Ask the spring-boot-expert agent to configure SSE streaming
> Use the llm-integration-expert to setup Ollama
```

### Skills Disponibles

Se activan automáticamente según el contexto:
- **angular21**: Desarrollo frontend con Signals, Zoneless
- **spring-ai-mcp**: Creación de tools con @Tool + DDD
- **clickhouse**: Queries, schema design, optimización
- **grok-integration**: Configuración Grok (dev/MVP gratuito)
- **ollama-integration**: Configuración Ollama (producción local)
- **retail-domain**: Métricas y conceptos de retail
- **ddd-clean-code**: Arquitectura DDD y principios Clean Code

---

## 📋 Checklist de Implementación

### Fase 1: Infraestructura
- [ ] Docker Compose con ClickHouse funcionando
- [ ] Schema creado con datos de prueba
- [ ] Cuenta Grok creada (gratuita) para desarrollo

### Fase 2: Backend (DDD + Clean Code + Gradle)
- [ ] Proyecto Spring Boot 3.4 con Gradle 8.11 creado
- [ ] Estructura DDD implementada (application, domain, infrastructure)
- [ ] Domain Layer: Entidades y Puertos (interfaces)
- [ ] Application Layer: Use Cases
- [ ] Infrastructure Layer: Adapters (ClickHouse, Grok, Ollama)
- [ ] Tools implementados (@Tool) usando Use Cases
- [ ] Integración con Grok (perfil dev)
- [ ] Integración con Ollama (perfil prod)
- [ ] Endpoint SSE streaming
- [ ] Tests unitarios por capa

### Fase 3: Frontend
- [ ] Proyecto Angular 21 creado
- [ ] Componente Chat con Signals
- [ ] Servicio SSE streaming
- [ ] UI con Tailwind CSS 4

### Fase 4: Integración y MVP
- [ ] Flujo completo end-to-end con Grok (dev)
- [ ] Demo MVP funcionando
- [ ] Ollama configurado para producción
- [ ] Tests de integración
- [ ] Documentación

---

## 🚀 Producción

### Infraestructura de Servicios

| Servicio | Puerto | Descripción |
|----------|--------|-------------|
| ClickHouse | 8123, 9000 | Base de datos OLAP |
| PostgreSQL | 5434 | Historial de conversaciones |
| Kafka | 9092 | Event streaming |
| Zookeeper | 2181 | Kafka coordination |
| Kafka UI | 8080 | Web interface para Kafka |
| Prometheus | 9090 | Métricas |
| Superset | 8088 | BI & Dashboards (Apache 2.0) |
| Ollama | 11434 | LLM local (producción) |
| Backend | 8081 | API Spring Boot |
| Frontend | 4200/80 | Angular UI |

### Iniciar Desarrollo

```bash
cd docker
docker-compose -f docker-compose.dev.yml up -d

# Verificar servicios
curl http://localhost:8123/ping          # ClickHouse
curl http://localhost:9090/-/healthy     # Prometheus
curl http://localhost:8088/health        # Superset
open http://localhost:8080               # Kafka UI
```

### Iniciar Producción

```bash
cd docker

# Preparar directorios
sudo mkdir -p /data/{clickhouse,postgres,kafka,zookeeper,prometheus,superset,ollama}
sudo mkdir -p /data/superset/{db,cache}

# Configurar
cp .env.prod.example .env.prod
nano .env.prod  # Editar con valores reales

# Deploy
docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d

# Verificar
docker-compose -f docker-compose.prod.yml ps
```

### Data Ingestion

El sistema soporta 3 métodos de ingesta:

| Método | Uso | Throughput |
|--------|-----|------------|
| **Kafka** (recomendado) | Streaming en tiempo real | Alto |
| **HTTP API** | Ingesta síncrona | Medio |
| **Batch** | Migración histórica | Variable |

Ver `docs/DATA-INGESTION.md` para detalles del SaleMapper y la integración con Geocom POS.

### Monitoreo

| Dashboard | URL | Credenciales |
|-----------|-----|--------------|
| Superset | http://localhost:8088 | admin/admin |
| Prometheus | http://localhost:9090 | - |
| Kafka UI | http://localhost:8080 | - |

### Skills y Agentes Adicionales

```
> Use the devops-expert agent for infrastructure tasks
> Use the kafka-integration skill for event streaming
> Use the monitoring skill for metrics and alerts
```

Ver `docs/PRODUCTION.md` para guía completa de despliegue
