# Spring AI MCP Skill (DDD + Clean Code)

## Overview

This skill provides knowledge for building MCP (Model Context Protocol) tools using Spring AI with **DDD (Domain-Driven Design)** architecture. Tools are exposed to LLMs (Grok/Ollama) and delegate to Use Cases following Clean Architecture.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    INFRASTRUCTURE LAYER                      │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  RetailAnalyticsTools.java (@Tool methods)          │    │
│  │  - Receives LLM tool calls                          │    │
│  │  - Delegates to Use Cases                           │    │
│  └──────────────────────┬──────────────────────────────┘    │
└─────────────────────────┼───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    APPLICATION LAYER                         │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  Use Cases (GetStoreSalesUseCase, etc.)             │    │
│  │  - Business logic validation                        │    │
│  │  - Orchestration                                    │    │
│  └──────────────────────┬──────────────────────────────┘    │
└─────────────────────────┼───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                      DOMAIN LAYER                            │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  SalesRepository (Interface/Port)                   │    │
│  │  Domain Models (SalesReport, StoreRanking, etc.)    │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

## Gradle Dependencies

```kotlin
// build.gradle.kts
dependencies {
    // Spring AI
    implementation("org.springframework.ai:spring-ai-openai-spring-boot-starter")  // Grok
    implementation("org.springframework.ai:spring-ai-ollama-spring-boot-starter")  // Ollama

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:1.0.0")
    }
}
```

## Tool Definition with DDD

### Infrastructure Layer - Tools

```java
package com.geocom.retail.infrastructure.tools;

import com.geocom.retail.application.usecase.GetStoreSalesUseCase;
import com.geocom.retail.application.usecase.GetStoresRankingUseCase;
import com.geocom.retail.domain.model.SalesReport;
import com.geocom.retail.domain.model.StoreRanking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * MCP Tools exposed to LLM.
 * Infrastructure layer - delegates to Application layer Use Cases.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RetailAnalyticsTools {

    private final GetStoreSalesUseCase getStoreSalesUseCase;
    private final GetStoresRankingUseCase getStoresRankingUseCase;

    @Tool(
        name = "get_store_sales",
        description = """
            Obtiene las ventas totales de un local especifico en un periodo de tiempo.
            Retorna: numero de tickets, ventas totales, ticket promedio.
            Usar cuando el usuario pregunta por ventas de un local.
            """
    )
    public SalesReport getStoreSales(
            @ToolParam(description = "ID del local, ejemplo: '001', '045'")
            String storeId,

            @ToolParam(description = "Fecha de inicio en formato YYYY-MM-DD")
            String startDate,

            @ToolParam(description = "Fecha de fin en formato YYYY-MM-DD")
            String endDate) {

        log.info("Tool get_store_sales: store={}, period={}-{}", storeId, startDate, endDate);

        return getStoreSalesUseCase.execute(
            storeId,
            LocalDate.parse(startDate),
            LocalDate.parse(endDate)
        );
    }

    @Tool(
        name = "get_stores_ranking",
        description = """
            Obtiene el ranking de locales ordenados por ventas en un periodo.
            Retorna: lista de locales con posicion, ventas y ticket promedio.
            Usar cuando preguntan por mejores/peores locales.
            """
    )
    public List<StoreRanking> getStoresRanking(
            @ToolParam(description = "Fecha de inicio YYYY-MM-DD")
            String startDate,

            @ToolParam(description = "Fecha de fin YYYY-MM-DD")
            String endDate,

            @ToolParam(description = "Numero maximo de locales (default: 10)")
            Integer limit) {

        int effectiveLimit = limit != null ? Math.min(limit, 100) : 10;

        log.info("Tool get_stores_ranking: period={}-{}, limit={}",
                 startDate, endDate, effectiveLimit);

        return getStoresRankingUseCase.execute(
            LocalDate.parse(startDate),
            LocalDate.parse(endDate),
            effectiveLimit
        );
    }
}
```

### Application Layer - Use Case

```java
package com.geocom.retail.application.usecase;

import com.geocom.retail.domain.exception.InvalidDateRangeException;
import com.geocom.retail.domain.exception.StoreNotFoundException;
import com.geocom.retail.domain.model.SalesReport;
import com.geocom.retail.domain.repository.SalesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Use Case for getting store sales.
 * Application layer - contains business validation logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetStoreSalesUseCase {

    private final SalesRepository salesRepository;

    public SalesReport execute(String storeId, LocalDate startDate, LocalDate endDate) {
        log.debug("Executing GetStoreSalesUseCase: store={}, period={}-{}",
                  storeId, startDate, endDate);

        // Business validation
        validateDateRange(startDate, endDate);
        validateStoreExists(storeId);

        return salesRepository.findSalesByStoreAndPeriod(storeId, startDate, endDate);
    }

    private void validateDateRange(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new InvalidDateRangeException("Start date must be before end date");
        }
        if (start.isBefore(LocalDate.now().minusYears(2))) {
            throw new InvalidDateRangeException("Cannot query data older than 2 years");
        }
    }

    private void validateStoreExists(String storeId) {
        if (!salesRepository.existsStore(storeId)) {
            throw new StoreNotFoundException("Store not found: " + storeId);
        }
    }
}
```

### Domain Layer - Model and Repository Interface

```java
// domain/model/SalesReport.java
package com.geocom.retail.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

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
package com.geocom.retail.domain.repository;

import com.geocom.retail.domain.model.SalesReport;
import java.time.LocalDate;

public interface SalesRepository {
    SalesReport findSalesByStoreAndPeriod(String storeId, LocalDate start, LocalDate end);
    boolean existsStore(String storeId);
}
```

### Infrastructure Layer - Repository Implementation

```java
package com.geocom.retail.infrastructure.adapter.out.persistence;

import com.geocom.retail.domain.model.SalesReport;
import com.geocom.retail.domain.repository.SalesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
@RequiredArgsConstructor
public class ClickHouseRepositoryImpl implements SalesRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public SalesReport findSalesByStoreAndPeriod(String storeId, LocalDate start, LocalDate end) {
        String sql = """
            SELECT store_id, count() as tickets, sum(total_amount) as sales,
                   avg(total_amount) as avg_ticket
            FROM fact_tickets
            WHERE store_id = ? AND ticket_date BETWEEN ? AND ?
            GROUP BY store_id
            """;

        return jdbcTemplate.queryForObject(sql, (rs, i) -> new SalesReport(
            rs.getString("store_id"),
            "Store " + rs.getString("store_id"),
            rs.getLong("tickets"),
            rs.getBigDecimal("sales"),
            rs.getBigDecimal("avg_ticket"),
            start,
            end
        ), storeId, start, end);
    }

    @Override
    public boolean existsStore(String storeId) {
        String sql = "SELECT count() > 0 FROM dim_stores WHERE store_id = ?";
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, storeId));
    }
}
```

## Application Configuration

```yaml
# application.yml
spring:
  application:
    name: geo-retail-analytics

  datasource:
    url: jdbc:ch://${CLICKHOUSE_HOST:localhost}:8123/retail_analytics
    driver-class-name: com.clickhouse.jdbc.ClickHouseDriver

---
# application-dev.yml (Grok - Free)
spring:
  ai:
    openai:
      base-url: https://api.x.ai/v1
      api-key: ${GROK_API_KEY}
      chat:
        model: grok-beta

---
# application-prod.yml (Ollama - Local)
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: llama3.1:8b
```

## Writing Effective Tool Descriptions

### Good Description Format

```java
@Tool(
    name = "get_store_sales",
    description = """
        Obtiene las ventas totales de un local especifico en un periodo.

        CUANDO USAR:
        - Cuando el usuario pregunta por ventas de un local
        - Cuando pregunta cuanto vendio una tienda
        - Cuando quiere saber ingresos o facturacion

        RETORNA:
        - Numero de tickets
        - Ventas totales en pesos
        - Ticket promedio

        EJEMPLO: "Cuanto vendio el local 001 en enero?"
        """
)
```

### Good Parameter Descriptions

```java
@ToolParam(
    description = """
        ID del local (3 digitos).
        Ejemplos: '001', '045', '123'
        """
)
String storeId
```

## Tool Design Best Practices

### 1. Delegate to Use Cases

```java
// GOOD - Tool delegates to Use Case
@Tool(name = "get_store_sales")
public SalesReport getStoreSales(...) {
    return getStoreSalesUseCase.execute(storeId, start, end);
}

// BAD - Business logic in Tool
@Tool(name = "get_store_sales")
public SalesReport getStoreSales(...) {
    if (start.isAfter(end)) { ... }  // Validation belongs in Use Case
    return repository.query(...);
}
```

### 2. Use Domain Models

```java
// GOOD - Return Domain model
public SalesReport getStoreSales(...) { ... }

// BAD - Return Map or raw data
public Map<String, Object> getStoreSales(...) { ... }
```

### 3. Handle Optional Parameters

```java
@Tool(name = "get_stores_ranking")
public List<StoreRanking> getStoresRanking(
        @ToolParam(description = "Limite (default: 10)")
        Integer limit) {

    int effectiveLimit = limit != null ? Math.min(limit, 100) : 10;
    return useCase.execute(..., effectiveLimit);
}
```

## Testing Tools

```java
@SpringBootTest
class RetailAnalyticsToolsTest {

    @Autowired
    private RetailAnalyticsTools tools;

    @MockBean
    private GetStoreSalesUseCase getStoreSalesUseCase;

    @Test
    void getStoreSales_validInput_delegatesToUseCase() {
        // Arrange
        var expected = new SalesReport("001", "Store 001", 1500,
            new BigDecimal("45000"), new BigDecimal("30"),
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        when(getStoreSalesUseCase.execute("001",
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31)))
            .thenReturn(expected);

        // Act
        var result = tools.getStoreSales("001", "2024-01-01", "2024-01-31");

        // Assert
        assertThat(result.ticketCount()).isEqualTo(1500);
        verify(getStoreSalesUseCase).execute(any(), any(), any());
    }
}
```

## Project Structure

```
backend/
├── src/main/java/com/geocom/retail/
│   ├── application/
│   │   ├── usecase/
│   │   │   ├── GetStoreSalesUseCase.java
│   │   │   └── GetStoresRankingUseCase.java
│   │   └── dto/
│   │       └── ...
│   ├── domain/
│   │   ├── model/
│   │   │   ├── SalesReport.java
│   │   │   └── StoreRanking.java
│   │   ├── repository/
│   │   │   └── SalesRepository.java
│   │   └── exception/
│   │       └── InvalidDateRangeException.java
│   └── infrastructure/
│       ├── adapter/out/persistence/
│       │   └── ClickHouseRepositoryImpl.java
│       └── tools/
│           └── RetailAnalyticsTools.java
└── build.gradle.kts
```

## References

- Spring AI: https://docs.spring.io/spring-ai/reference/
- Spring AI Tools: https://docs.spring.io/spring-ai/reference/api/tools.html
- Gradle: https://docs.gradle.org/8.11/userguide/userguide.html
- DDD: https://martinfowler.com/bliki/DomainDrivenDesign.html
