# Backend - Spring Boot + DDD + Clean Code

## Descripcion

Backend unificado con arquitectura **Domain-Driven Design (DDD)** y principios **Clean Code**:

- **Domain Layer**: Entidades de negocio y puertos (interfaces)
- **Application Layer**: Use Cases y orquestacion
- **Infrastructure Layer**: Adapters (Controllers, Repositories, LLM clients)

## Stack

- **Java**: 21 LTS
- **Spring Boot**: 3.4.x
- **Build Tool**: Gradle 8.11.x
- **Spring AI**: 1.0.x (para LLM y @Tool)
- **ClickHouse JDBC**: 0.7.x

## Integraciones LLM

| Perfil | LLM Provider | Uso | Configuracion |
|--------|--------------|-----|---------------|
| `dev` | **Grok (xAI)** | Desarrollo y MVP | Gratuito, API compatible OpenAI |
| `prod` | **Ollama** | Produccion | Local, datos sensibles seguros |

## Estructura DDD

```
src/main/java/com/geocom/retail/
├── RetailAnalyticsApp.java           # Main
│
├── application/                      # CAPA APPLICATION
│   ├── service/
│   │   └── ChatService.java                  # Orquesta LLM + Tools
│   ├── usecase/
│   │   ├── GetStoreSalesUseCase.java
│   │   ├── GetStoresRankingUseCase.java
│   │   ├── GetTopProductsUseCase.java
│   │   └── ComparePeriodsUseCase.java
│   ├── dto/
│   │   ├── ChatRequest.java
│   │   └── ChatResponse.java
│   └── port/
│       └── LlmPort.java                      # Puerto para LLM
│
├── domain/                           # CAPA DOMAIN
│   ├── model/
│   │   ├── Store.java
│   │   ├── Ticket.java
│   │   ├── SalesReport.java
│   │   └── StoreRanking.java
│   ├── repository/                   # Puertos (Interfaces)
│   │   └── SalesRepository.java
│   ├── service/
│   │   └── SalesAnalyticsDomainService.java
│   └── exception/
│       ├── InvalidDateRangeException.java
│       └── StoreNotFoundException.java
│
└── infrastructure/                   # CAPA INFRASTRUCTURE
    ├── adapter/
    │   ├── in/
    │   │   └── api/
    │   │       ├── ChatController.java       # REST + SSE
    │   │       └── HealthController.java
    │   └── out/
    │       ├── persistence/
    │       │   └── ClickHouseRepositoryImpl.java
    │       └── llm/
    │           ├── GrokLlmAdapter.java       # Dev/MVP (Gratuito)
    │           └── OllamaLlmAdapter.java     # Produccion (Local)
    ├── config/
    │   ├── LlmConfig.java
    │   └── ClickHouseConfig.java
    └── tools/
        └── RetailAnalyticsTools.java         # @Tool methods
```

## Principios Clean Code Aplicados

1. **Single Responsibility**: Cada clase tiene una unica responsabilidad
2. **Dependency Inversion**: Domain define puertos, Infrastructure implementa
3. **Interface Segregation**: Interfaces pequenas y especificas
4. **Inmutabilidad**: Records para DTOs y Value Objects
5. **Fail Fast**: Validacion temprana en Use Cases
6. **Nombres Expresivos**: Codigo auto-documentado

## Setup

### Desarrollo (Grok - Gratuito)

```bash
# 1. Obtener API Key en https://console.x.ai (gratuito)
export GROK_API_KEY=your_api_key

# 2. Levantar ClickHouse
docker compose -f ../docker/docker-compose.dev.yml up -d

# 3. Iniciar backend
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Produccion (Ollama - Local)

```bash
# 1. Instalar Ollama
curl -fsSL https://ollama.com/install.sh | sh

# 2. Descargar modelo
ollama pull llama3.1:8b

# 3. Iniciar backend
./gradlew bootRun --args='--spring.profiles.active=prod'
```

## Configuracion

```yaml
# application.yml
spring:
  application:
    name: geo-retail-analytics

  datasource:
    url: jdbc:ch://localhost:8123/retail_analytics
    driver-class-name: com.clickhouse.jdbc.ClickHouseDriver

server:
  port: 8080

---
# application-dev.yml (Grok - Gratuito)
spring:
  ai:
    openai:
      base-url: https://api.x.ai/v1
      api-key: ${GROK_API_KEY}
      chat:
        model: grok-beta
        options:
          temperature: 0.7

---
# application-prod.yml (Ollama - Local)
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: llama3.1:8b
        options:
          temperature: 0.7
          num-predict: 2000
```

## Endpoints

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST | `/api/chat` | Chat sincrono |
| POST | `/api/chat/stream` | Chat con streaming SSE |
| GET | `/actuator/health` | Health check |

## Tools Disponibles

Los tools estan definidos en `RetailAnalyticsTools.java` y usan Use Cases:

| Tool | Use Case | Descripcion |
|------|----------|-------------|
| `get_store_sales` | GetStoreSalesUseCase | Ventas de un local en periodo |
| `get_stores_ranking` | GetStoresRankingUseCase | Ranking de locales |
| `get_top_products` | GetTopProductsUseCase | Productos mas vendidos |
| `get_hourly_traffic` | GetHourlyTrafficUseCase | Trafico por hora |
| `compare_periods` | ComparePeriodsUseCase | Comparar dos periodos |

## Perfiles

| Perfil | LLM | Comando |
|--------|-----|---------|
| `dev` | Grok (xAI) gratuito | `./gradlew bootRun --args='--spring.profiles.active=dev'` |
| `prod` | Ollama local | `./gradlew bootRun --args='--spring.profiles.active=prod'` |

## Dependencias Gradle

```kotlin
// build.gradle.kts
plugins {
    java
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.6"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Spring AI
    implementation("org.springframework.ai:spring-ai-openai-spring-boot-starter")  // Grok compatible
    implementation("org.springframework.ai:spring-ai-ollama-spring-boot-starter")

    // ClickHouse
    implementation("com.clickhouse:clickhouse-jdbc:0.7.0:all")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:1.0.0")
    }
}
```

## Comandos Gradle

```bash
# Build
./gradlew build

# Run con perfil dev (Grok)
./gradlew bootRun --args='--spring.profiles.active=dev'

# Run con perfil prod (Ollama)
./gradlew bootRun --args='--spring.profiles.active=prod'

# Tests
./gradlew test

# Clean
./gradlew clean

# Check dependencies
./gradlew dependencies
```

## Variables de Entorno

| Variable | Descripcion | Perfil |
|----------|-------------|--------|
| `GROK_API_KEY` | API Key de Grok (xAI) | dev |
| `CLICKHOUSE_HOST` | Host de ClickHouse | todos |
| `OLLAMA_BASE_URL` | URL de Ollama | prod |

## Testing

```java
// Test de Use Case
@SpringBootTest
class GetStoreSalesUseCaseTest {

    @Autowired
    private GetStoreSalesUseCase useCase;

    @MockBean
    private SalesRepository salesRepository;

    @Test
    void execute_validInput_returnsSalesReport() {
        // Arrange
        when(salesRepository.findSalesByStoreAndPeriod("001", start, end))
            .thenReturn(expectedReport);

        // Act
        SalesReport result = useCase.execute("001", start, end);

        // Assert
        assertThat(result.totalSales()).isEqualByComparingTo("45000.00");
    }

    @Test
    void execute_invalidDateRange_throwsException() {
        assertThatThrownBy(() -> useCase.execute("001", end, start))
            .isInstanceOf(InvalidDateRangeException.class);
    }
}
```
