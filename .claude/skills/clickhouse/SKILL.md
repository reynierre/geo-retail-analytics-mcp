# ClickHouse Skill

## Overview

This skill provides knowledge for designing schemas, writing queries, and optimizing performance in ClickHouse for retail analytics workloads based on the **Sale Domain** (Geocom POS).

## ClickHouse Basics

### Why ClickHouse for Analytics

| Traditional RDBMS | ClickHouse |
|-------------------|------------|
| Row-based storage | Column-based storage |
| Read entire rows | Read only needed columns |
| Good for OLTP | Optimized for OLAP |
| Single-threaded queries | Parallel query execution |
| Standard compression | Aggressive compression (6-10x) |

### When to Use ClickHouse

✅ **Good for:**
- Aggregations over millions/billions of rows
- Time-series data analysis
- Real-time analytics dashboards
- Log analysis
- Event tracking

❌ **Not good for:**
- High-frequency UPDATE/DELETE
- Point queries by primary key
- ACID transactions
- Complex JOINs
- Blob storage

## Schema Design (Sale Domain)

### Fact Tables

```sql
-- FACT: Sales (Aggregate Root)
CREATE TABLE IF NOT EXISTS fact_sales (
    company_id UInt64,
    store_id UInt64,
    pos_terminal_id LowCardinality(String),
    ticket_number String,
    sale_date DateTime,

    active UInt8 DEFAULT 1,
    cashier_id LowCardinality(String),
    currency_code LowCardinality(String) DEFAULT 'UYU',

    subtotal_amount Decimal(12,2),
    discount_amount Decimal(12,2),
    tax_amount Decimal(12,2),
    rounding_amount Decimal(12,2) DEFAULT 0,
    surcharge_amount Decimal(12,2) DEFAULT 0,
    total_amount Decimal(12,2),

    fiscal_document_type_id LowCardinality(String),
    fiscal_document_series String,
    fiscal_document_number String,
    customer_tax_type_id UInt32 DEFAULT 0,
    customer_tax_number Nullable(String)
)
ENGINE = MergeTree()
PARTITION BY toYYYYMM(sale_date)
ORDER BY (company_id, store_id, sale_date, pos_terminal_id, ticket_number)
TTL sale_date + INTERVAL 5 YEAR DELETE;

-- FACT: Sale Items
CREATE TABLE IF NOT EXISTS fact_sale_items (
    company_id UInt64,
    store_id UInt64,
    pos_terminal_id LowCardinality(String),
    ticket_number String,
    sale_date DateTime,
    line_number UInt16,

    product_id UInt64,
    barcode String,
    quantity Decimal(10,3),
    gross_unit_price Decimal(12,2),
    discount_amount Decimal(12,2) DEFAULT 0,
    net_unit_price Decimal(12,2),
    subtotal_amount Decimal(12,2),
    tax_amount Decimal(12,2),
    tax_code LowCardinality(String),
    total_amount Decimal(12,2)
)
ENGINE = MergeTree()
PARTITION BY toYYYYMM(sale_date)
ORDER BY (company_id, store_id, sale_date, pos_terminal_id, ticket_number, line_number);

-- FACT: Sale Payments
CREATE TABLE IF NOT EXISTS fact_sale_payments (
    company_id UInt64,
    store_id UInt64,
    pos_terminal_id LowCardinality(String),
    ticket_number String,
    sale_date DateTime,
    payment_sequence UInt8,

    payment_method_id LowCardinality(String),
    currency_code LowCardinality(String) DEFAULT 'UYU',
    tendered_amount Decimal(12,2),
    total_amount Decimal(12,2),
    discount_amount Decimal(12,2) DEFAULT 0,

    card_brand_id Nullable(String),
    card_brand_name LowCardinality(Nullable(String)),
    card_type Nullable(FixedString(1)),        -- C=Credit, D=Debit
    card_entry_mode Nullable(FixedString(1)),  -- C=Chip, M=Magnetic, F=NFC
    installment_count UInt8 DEFAULT 1,
    vat_discount_applied UInt8 DEFAULT 0,
    tax_law_code Nullable(String)
)
ENGINE = MergeTree()
PARTITION BY toYYYYMM(sale_date)
ORDER BY (company_id, store_id, sale_date, pos_terminal_id, ticket_number, payment_sequence);
```

### Dimension Tables

```sql
-- DIMENSION: Stores
CREATE TABLE IF NOT EXISTS dim_stores (
    store_id UInt64,
    company_id UInt64,
    store_code String,
    store_name String,
    region LowCardinality(String),
    city String,
    store_format LowCardinality(String),
    active UInt8 DEFAULT 1,
    updated_at DateTime DEFAULT now()
)
ENGINE = ReplacingMergeTree(updated_at)
ORDER BY (company_id, store_id);

-- DIMENSION: Products
CREATE TABLE IF NOT EXISTS dim_products (
    product_id UInt64,
    company_id UInt64,
    barcode String,
    product_name String,
    category LowCardinality(String),
    subcategory LowCardinality(String),
    brand LowCardinality(String),
    active UInt8 DEFAULT 1,
    updated_at DateTime DEFAULT now()
)
ENGINE = ReplacingMergeTree(updated_at)
ORDER BY (company_id, product_id);

-- DIMENSION: Payment Methods
CREATE TABLE IF NOT EXISTS dim_payment_methods (
    payment_method_id String,
    payment_method_name String,
    payment_type LowCardinality(String),  -- CASH, CARD, CHECK, ACCOUNT
    active UInt8 DEFAULT 1,
    updated_at DateTime DEFAULT now()
)
ENGINE = ReplacingMergeTree(updated_at)
ORDER BY payment_method_id;
```

### Materialized Views

```sql
-- Daily Sales Summary
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_daily_sales
ENGINE = SummingMergeTree()
PARTITION BY toYYYYMM(sale_date)
ORDER BY (company_id, store_id, sale_date)
AS SELECT
    company_id,
    store_id,
    toDate(sale_date) AS sale_date,
    count() AS ticket_count,
    sum(subtotal_amount) AS total_subtotal,
    sum(discount_amount) AS total_discounts,
    sum(tax_amount) AS total_taxes,
    sum(total_amount) AS total_sales
FROM fact_sales
WHERE active = 1
GROUP BY company_id, store_id, toDate(sale_date);

-- Payment Method Summary
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_payment_summary
ENGINE = SummingMergeTree()
PARTITION BY toYYYYMM(sale_date)
ORDER BY (company_id, store_id, sale_date, payment_method_id)
AS SELECT
    company_id,
    store_id,
    toDate(sale_date) AS sale_date,
    payment_method_id,
    count() AS payment_count,
    sum(total_amount) AS total_amount,
    sum(discount_amount) AS total_discounts
FROM fact_sale_payments
WHERE active = 1
GROUP BY company_id, store_id, toDate(sale_date), payment_method_id;
```

## Query Patterns

### Store Sales

```sql
SELECT
    s.store_id,
    st.store_name,
    count() AS ticket_count,
    sum(s.total_amount) AS total_sales,
    avg(s.total_amount) AS avg_ticket
FROM fact_sales s
LEFT JOIN dim_stores st ON s.store_id = st.store_id
WHERE s.company_id = {company_id}
  AND s.sale_date >= {start_date}
  AND s.sale_date < {end_date} + INTERVAL 1 DAY
  AND s.active = 1
GROUP BY s.store_id, st.store_name
ORDER BY total_sales DESC;
```

### Payment Analysis

```sql
SELECT
    p.payment_method_id,
    pm.payment_method_name,
    count() AS payment_count,
    sum(p.total_amount) AS total_amount,
    sum(p.discount_amount) AS total_discounts,
    avg(p.installment_count) AS avg_installments
FROM fact_sale_payments p
LEFT JOIN dim_payment_methods pm ON p.payment_method_id = pm.payment_method_id
WHERE p.company_id = {company_id}
  AND p.sale_date >= {start_date}
  AND p.active = 1
GROUP BY p.payment_method_id, pm.payment_method_name;
```

### Card Brand Analysis

```sql
SELECT
    p.card_brand_id,
    p.card_brand_name,
    count() AS transactions,
    sum(p.total_amount) AS total,
    sum(if(p.vat_discount_applied, p.discount_amount, 0)) AS vat_discounts,
    avg(p.installment_count) AS avg_cuotas
FROM fact_sale_payments p
WHERE p.company_id = {company_id}
  AND p.card_brand_id IS NOT NULL
  AND p.active = 1
GROUP BY p.card_brand_id, p.card_brand_name;
```

### Product Sales

```sql
SELECT
    i.product_id,
    pr.product_name,
    pr.category,
    sum(i.quantity) AS units_sold,
    sum(i.total_amount) AS revenue,
    sum(i.discount_amount) AS discounts
FROM fact_sale_items i
LEFT JOIN dim_products pr ON i.product_id = pr.product_id
WHERE i.company_id = {company_id}
  AND i.sale_date >= {start_date}
  AND i.active = 1
GROUP BY i.product_id, pr.product_name, pr.category
ORDER BY revenue DESC
LIMIT 20;
```

## Date Functions

```sql
-- Common date operations
toDate(sale_date)                     -- DateTime to Date
toYYYYMM(sale_date)                   -- 202401
toYear(sale_date)                     -- 2024
toMonth(sale_date)                    -- 1-12
toHour(sale_date)                     -- 0-23
toDayOfWeek(sale_date)                -- 1-7 (Mon-Sun)

-- Date arithmetic
sale_date + INTERVAL 1 DAY
sale_date - INTERVAL 1 MONTH
dateDiff('day', start_date, end_date)

-- Date ranges
WHERE sale_date >= '2024-01-01'
  AND sale_date < '2024-02-01' -- Excludes Feb 1
```

## Performance Optimization

### PREWHERE Clause

```sql
-- Filter before reading other columns
SELECT *
FROM fact_sales
PREWHERE store_id = 1  -- Applied first
WHERE total_amount > 100;   -- Applied after
```

### Query Analysis

```sql
-- Check query execution
EXPLAIN SELECT ... ;

-- Check query stats
SELECT
    query,
    read_rows,
    read_bytes,
    memory_usage,
    query_duration_ms
FROM system.query_log
WHERE type = 'QueryFinish'
ORDER BY event_time DESC
LIMIT 10;
```

### Index Optimization

```sql
-- Check if query uses index
-- Good: Uses index prefix (company_id, store_id, sale_date)
WHERE company_id = 1 AND store_id = 1 AND sale_date >= '2024-01-01'

-- Bad: Doesn't use index (skips company_id)
WHERE sale_date >= '2024-01-01'

-- Bad: Using function on indexed column
WHERE toDate(sale_date) = '2024-01-01'
-- Better:
WHERE sale_date >= '2024-01-01' AND sale_date < '2024-01-02'
```

## Spring Boot Integration

### Dependencies (Gradle)

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.clickhouse:clickhouse-jdbc:0.7.0:all")
}
```

### Configuration

```yaml
spring:
  datasource:
    url: jdbc:ch://localhost:8123/geo_retail_analytics
    driver-class-name: com.clickhouse.jdbc.ClickHouseDriver
    hikari:
      maximum-pool-size: 10
      connection-timeout: 5000
```

### Repository

```java
@Repository
@RequiredArgsConstructor
public class ClickHouseRepository {

    private final JdbcTemplate jdbc;

    public StoreSalesResult getStoreSales(Long companyId, Long storeId, String start, String end) {
        return jdbc.queryForObject("""
            SELECT store_id, count() as tickets, sum(total_amount) as sales
            FROM fact_sales
            WHERE company_id = ? AND store_id = ?
              AND sale_date >= ? AND sale_date < ? + INTERVAL 1 DAY
              AND active = 1
            GROUP BY store_id
            """,
            (rs, i) -> new StoreSalesResult(
                rs.getLong("store_id"),
                rs.getLong("tickets"),
                rs.getBigDecimal("sales")
            ),
            companyId, storeId, start, end
        );
    }
}
```

## Docker Setup

```yaml
# docker-compose.yml
services:
  clickhouse:
    image: clickhouse/clickhouse-server:24-alpine
    ports:
      - "8123:8123"  # HTTP
      - "9000:9000"  # Native
    volumes:
      - clickhouse_data:/var/lib/clickhouse
      - ./database/schema:/docker-entrypoint-initdb.d
    environment:
      CLICKHOUSE_DB: geo_retail_analytics
      CLICKHOUSE_USER: default
      CLICKHOUSE_PASSWORD: ""
    ulimits:
      nofile:
        soft: 262144
        hard: 262144

volumes:
  clickhouse_data:
```

## Monitoring

```sql
-- Table sizes
SELECT
    table,
    formatReadableSize(sum(bytes_on_disk)) AS size,
    sum(rows) AS rows
FROM system.parts
WHERE active AND database = 'geo_retail_analytics'
GROUP BY table;

-- Compression ratio
SELECT
    table,
    round(sum(data_uncompressed_bytes) / sum(data_compressed_bytes), 2) AS ratio
FROM system.parts
WHERE active
GROUP BY table;

-- Slow queries
SELECT query, query_duration_ms, read_rows
FROM system.query_log
WHERE query_duration_ms > 1000
ORDER BY event_time DESC
LIMIT 10;
```

## References

- ClickHouse Docs: https://clickhouse.com/docs
- SQL Reference: https://clickhouse.com/docs/en/sql-reference
- JDBC Driver: https://github.com/ClickHouse/clickhouse-java
