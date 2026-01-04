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
│  Chat UI con Signals, Zoneless, Control Flow (@if/@for), Tailwind CSS 4     │
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
| **Frontend** | Angular | 21.x | MIT | Chat UI con Signals + Zoneless |
| **Styling** | Tailwind CSS | 4.x | MIT | Utility-first CSS |
| **Backend** | Spring Boot | 3.4.x | Apache 2.0 | API + LLM + Tools (todo junto) |
| **Build Tool** | Gradle | 8.11.x | Apache 2.0 | Build y gestión de dependencias |
| **LLM Tools** | Spring AI | 1.0.x | Apache 2.0 | Anotación @Tool para tools |
| **LLM (Dev/MVP)** | Grok (xAI) | Free Tier | Gratuito | LLM gratuito para desarrollo y MVP |
| **LLM (Prod)** | Ollama + Llama 3.1 | 8B/70B | Apache 2.0 | LLM local para datos de producción |
| **Analytics DB** | ClickHouse | 24.x | Apache 2.0 | Queries analíticas ultra-rápidas |
| **BI Dashboard** | Apache Superset | 3.x | Apache 2.0 | Dashboards interactivos |
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
│   │   ├── devops-expert.md           # Experto en DevOps
│   │   └── llm-integration-expert.md  # Experto en integración LLM
│   └── skills/                        # Skills del proyecto
│       ├── angular21/SKILL.md
│       ├── spring-ai-mcp/SKILL.md
│       ├── clickhouse/SKILL.md
│       ├── kafka-integration/SKILL.md
│       ├── monitoring/SKILL.md
│       ├── ollama-integration/SKILL.md
│       └── retail-domain/SKILL.md
├── frontend/                          # Angular 21 Chat UI
│   ├── src/app/
│   │   ├── app.ts                     # Root component
│   │   ├── app.config.ts              # Zoneless + providers
│   │   ├── app.routes.ts
│   │   ├── features/
│   │   │   ├── chat/
│   │   │   │   ├── chat.ts            # Main chat component
│   │   │   │   ├── components/
│   │   │   │   └── services/
│   │   │   └── shared/                # 2+ features only
│   │   │       ├── components/
│   │   │       ├── pipes/
│   │   │       └── directives/
│   │   └── core/                      # Singleton services
│   │       ├── services/
│   │       └── interceptors/
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
│   ├── docker-compose.dev.yml         # ClickHouse + Kafka + Prometheus + Superset
│   ├── docker-compose.prod.yml        # Producción con recursos
│   ├── prometheus/
│   │   └── prometheus.yml
│   ├── superset/
│   │   └── superset_config.py
│   └── .env.example
└── docs/
    ├── ARCHITECTURE.md
    ├── PRODUCTION.md
    └── DATA-INGESTION.md
```

### ¿Por qué `database/`?

La carpeta `database/schema/` contiene scripts SQL que:
1. **Se versionan en Git** - Cambios al schema son trazables
2. **Se ejecutan automáticamente** - Docker los corre al iniciar ClickHouse
3. **Incluyen datos de prueba** - 500K tickets listos para desarrollo

---

## 🎨 Frontend: Angular 21 Modern Patterns

### Features Clave de Angular 21

| Feature | Descripción |
|---------|-------------|
| **Signals** | `signal()`, `computed()`, `effect()` para estado reactivo |
| **linkedSignal** | Estado dependiente que puede ser modificado |
| **resource()** | Async data fetching reactivo |
| **Zoneless** | `provideZonelessChangeDetection()` - sin Zone.js |
| **Control Flow** | `@if`, `@for`, `@switch`, `@defer` nativo |
| **input()/output()** | Funciones en lugar de decoradores |
| **inject()** | DI sin constructor |
| **Standalone** | Default - no necesita `standalone: true` |

### App Configuration

```typescript
// app.config.ts
import { ApplicationConfig, provideZonelessChangeDetection } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZonelessChangeDetection(),  // Sin Zone.js
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withFetch(), withInterceptors([authInterceptor]))
  ]
};
```

### Component Pattern

```typescript
import { Component, signal, computed, inject, input, output, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-chat',
  // standalone: true NO necesario - es default
  imports: [ChatMessageComponent, SpinnerComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    'class': 'flex flex-col h-screen',
    '[class.is-loading]': 'isLoading()'
  },
  template: `
    @for (message of messages(); track message.id) {
      <app-chat-message [message]="message" />
    } @empty {
      <p>No messages</p>
    }
    
    @if (isStreaming()) {
      <div>{{ streamingContent() }}<span class="animate-pulse">▊</span></div>
    }
    
    @defer (on viewport) {
      <app-suggestions />
    } @placeholder {
      <div class="skeleton h-24" />
    }
  `
})
export class ChatComponent {
  private readonly chatService = inject(ChatService);
  
  // Inputs con funciones
  readonly disabled = input(false);
  
  // Outputs con funciones
  readonly messageSent = output<string>();
  
  // Signals para estado
  protected readonly messages = this.chatService.messages;
  protected readonly isStreaming = signal(false);
  protected readonly streamingContent = signal('');
  
  // Computed para estado derivado
  protected readonly hasMessages = computed(() => this.messages().length > 0);
}
```

### Best Practices Angular 21

#### ✅ DO
- Usar `signal()` para todo estado
- Usar `computed()` para estado derivado
- Usar `inject()` en lugar de constructor
- Usar `input()` / `output()` en lugar de decoradores
- Usar `@if` / `@for` / `@switch` / `@defer`
- Usar `track` en todos los `@for`
- Usar `host` object en lugar de `@HostBinding`
- Usar `OnPush` change detection

#### ❌ DON'T
- No usar `standalone: true` - es default
- No usar `NgModules` para features
- No usar `@Input()` / `@Output()` decoradores
- No usar `*ngIf` / `*ngFor` directivas
- No usar `ngClass` / `ngStyle`
- No usar `@HostBinding` / `@HostListener`
- No usar `any` type
- No olvidar `track` en `@for`

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

### RetailAnalyticsTools.java

```java
@Component
public class RetailAnalyticsTools {
    
    private final GetStoreSalesUseCase getStoreSalesUseCase;
    private final GetStoresRankingUseCase getStoresRankingUseCase;
    
    @Tool(description = "Get total sales for a specific store in a date range")
    public SalesReport getStoreSales(
        @ToolParam(description = "Store ID (e.g., '001')") String storeId,
        @ToolParam(description = "Start date (YYYY-MM-DD)") String startDate,
        @ToolParam(description = "End date (YYYY-MM-DD)") String endDate
    ) {
        return getStoreSalesUseCase.execute(storeId, 
            LocalDate.parse(startDate), LocalDate.parse(endDate));
    }
    
    @Tool(description = "Get ranking of stores by total sales")
    public List<StoreRanking> getStoresRanking(
        @ToolParam(description = "Start date") String startDate,
        @ToolParam(description = "End date") String endDate,
        @ToolParam(description = "Number of results") int limit
    ) {
        return getStoresRankingUseCase.execute(
            LocalDate.parse(startDate), LocalDate.parse(endDate), limit);
    }
}
```

---

## 🚀 Comandos de Desarrollo

```bash
# === DOCKER (Infraestructura) ===

# Levantar servicios de desarrollo
cd docker
docker-compose -f docker-compose.dev.yml up -d

# Verificar servicios
curl http://localhost:8123/ping          # ClickHouse
curl http://localhost:9090/-/healthy     # Prometheus
curl http://localhost:8088/health        # Superset
open http://localhost:8080               # Kafka UI

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

# === FRONTEND (Angular 21) ===

cd frontend
npm install
ng serve  # Abre http://localhost:4200

# Tests con Vitest
npm test

# Build producción
ng build --configuration=production

# === VERIFICAR ===

# ClickHouse
docker exec -it retail-clickhouse clickhouse-client -q "SELECT count() FROM geo_retail_analytics.fact_sales"

# Backend health
curl http://localhost:8081/actuator/health

# Chat test
curl -X POST http://localhost:8081/api/chat \
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
> Use the devops-expert agent for infrastructure tasks
```

### Skills Disponibles

Se activan automáticamente según el contexto:
- **angular21**: Desarrollo frontend con Signals, Zoneless, Control Flow
- **spring-ai-mcp**: Creación de tools con @Tool + DDD
- **clickhouse**: Queries, schema design, optimización
- **kafka-integration**: Event streaming
- **monitoring**: Métricas con Prometheus + Superset
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
- [ ] Kafka + Zookeeper configurados
- [ ] Prometheus + Superset configurados

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

### Fase 3: Frontend (Angular 21)
- [ ] Proyecto Angular 21 creado con Zoneless
- [ ] Componente Chat con Signals
- [ ] Servicio SSE streaming con fetch API
- [ ] UI con Tailwind CSS 4
- [ ] Control Flow (@if, @for, @defer)
- [ ] Tests con Vitest

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
