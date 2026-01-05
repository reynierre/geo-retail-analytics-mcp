# Monitoring Skill

## Overview

Stack de monitoreo con Prometheus y Apache Superset para geo-retail-analytics.

## Components

| Component | Port | Purpose | License |
|-----------|------|---------|---------|
| Prometheus | 9090 | Metrics collection & storage | Apache 2.0 |
| Superset | 8088 | BI & Dashboards | Apache 2.0 |
| Spring Actuator | 8081/actuator | Application metrics | Apache 2.0 |
| ClickHouse | 8123/metrics | Database metrics | Apache 2.0 |

## Access URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| Prometheus | http://localhost:9090 | - |
| Superset | http://localhost:8088 | admin/admin |
| Kafka UI | http://localhost:8080 | - |

## Spring Boot Actuator Configuration

### Gradle Dependencies

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
}
```

### Application Configuration

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
      base-path: /actuator
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: geo-retail-analytics
      environment: ${SPRING_PROFILES_ACTIVE:dev}
```

## Key Metrics

### Application Metrics (PromQL)

```promql
# Request rate (per second)
rate(http_server_requests_seconds_count[5m])

# Response time P95
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Response time P99
histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))

# Error rate (5xx)
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# Success rate
sum(rate(http_server_requests_seconds_count{status=~"2.."}[5m])) /
sum(rate(http_server_requests_seconds_count[5m]))
```

### ClickHouse Metrics

```promql
# Queries per second
rate(ClickHouseProfileEvents_Query[5m])

# Insert rows per second
rate(ClickHouseProfileEvents_InsertedRows[5m])

# Memory usage
ClickHouseMetrics_MemoryTracking

# Active connections
ClickHouseMetrics_TCPConnection
```

### Kafka Metrics

```promql
# Consumer lag (critical for real-time processing)
kafka_consumer_lag

# Messages in per second
rate(kafka_server_brokertopicmetrics_messagesin_total[5m])

# Bytes in per second
rate(kafka_server_brokertopicmetrics_bytesin_total[5m])

# Partition count
kafka_server_replicamanager_partitioncount

# Under-replicated partitions (should be 0)
kafka_server_replicamanager_underreplicatedpartitions
```

### Business Metrics

```promql
# Sales processed per minute
rate(sales_processed_total[1m])

# Average ticket value
sales_total_amount_sum / sales_processed_total

# Payment method distribution
sum by (payment_method) (rate(payments_processed_total[5m]))

# Sales by store
sum by (store_id) (rate(sales_processed_total[5m]))
```

## Custom Business Metrics

### SalesMetrics.java

```java
package uy.com.geocom.retail.infrastructure.metrics;

@Component
@RequiredArgsConstructor
public class SalesMetrics {

    private final MeterRegistry registry;

    private Counter salesCounter;
    private Counter paymentsCounter;
    private DistributionSummary ticketAmountSummary;
    private Timer queryTimer;

    @PostConstruct
    public void init() {
        salesCounter = Counter.builder("sales.processed")
            .description("Total sales processed")
            .register(registry);

        paymentsCounter = Counter.builder("payments.processed")
            .description("Total payments processed")
            .tag("payment_method", "unknown")
            .register(registry);

        ticketAmountSummary = DistributionSummary.builder("sales.ticket.amount")
            .description("Ticket amount distribution")
            .baseUnit("UYU")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);

        queryTimer = Timer.builder("clickhouse.query")
            .description("ClickHouse query duration")
            .register(registry);
    }

    public void recordSale(Sale sale) {
        salesCounter.increment();
        ticketAmountSummary.record(sale.getTotalAmount().doubleValue());

        // Record payments by method
        sale.getPayments().forEach(payment -> {
            Counter.builder("payments.processed")
                .tag("payment_method", payment.getPaymentMethodId())
                .tag("store_id", String.valueOf(sale.getStoreId()))
                .register(registry)
                .increment();
        });
    }

    public Timer.Sample startQueryTimer() {
        return Timer.start(registry);
    }

    public void stopQueryTimer(Timer.Sample sample, String queryType) {
        sample.stop(Timer.builder("clickhouse.query")
            .tag("type", queryType)
            .register(registry));
    }
}
```

## Alerts Configuration

### Prometheus Alert Rules

```yaml
# alerts.yml
groups:
  - name: geo-retail-alerts
    rules:
      - alert: ClickHouseDown
        expr: up{job="clickhouse"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "ClickHouse is down"
          description: "ClickHouse has been down for more than 1 minute"

      - alert: KafkaConsumerLagHigh
        expr: kafka_consumer_lag > 10000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Kafka consumer lag is high"
          description: "Consumer lag is {{ $value }} messages behind"

      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value | humanizePercentage }}"

      - alert: SlowQueries
        expr: histogram_quantile(0.95, rate(clickhouse_query_duration_seconds_bucket[5m])) > 2
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Slow ClickHouse queries"
          description: "P95 query time is {{ $value }}s"

      - alert: HighMemoryUsage
        expr: process_resident_memory_bytes / 1024 / 1024 / 1024 > 6
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage"
          description: "Memory usage is {{ $value }}GB"
```

## Superset Dashboards

### Initial Setup

1. Open http://localhost:8088
2. Login: admin / admin
3. Go to Settings -> Database Connections
4. Add ClickHouse connection:
   - Database Name: ClickHouse
   - SQLAlchemy URI: `clickhousedb://default:@clickhouse:8123/geo_retail_analytics`
5. Create datasets from tables
6. Build dashboards

### Pre-built Queries

#### Daily Sales

```sql
SELECT
    the_date,
    sum(total_sales) as total_sales,
    sum(ticket_count) as tickets
FROM mv_daily_sales
WHERE the_date >= today() - 30
GROUP BY the_date
ORDER BY the_date
```

#### Payment Methods Distribution

```sql
SELECT
    payment_method_id,
    sum(total_amount) as total,
    count() as transactions
FROM mv_payment_summary
WHERE the_date >= today() - 7
GROUP BY payment_method_id
ORDER BY total DESC
```

#### Hourly Traffic

```sql
SELECT
    the_hour,
    sum(ticket_count) as tickets,
    sum(total_sales) as sales
FROM mv_hourly_sales
WHERE the_date = today()
GROUP BY the_hour
ORDER BY the_hour
```

#### Product Performance

```sql
SELECT
    p.product_name,
    ps.total_quantity,
    ps.total_revenue,
    ps.total_revenue / ps.total_quantity as avg_price
FROM mv_product_sales ps
JOIN dim_products p ON ps.product_id = p.product_id
WHERE ps.the_date >= today() - 7
ORDER BY ps.total_revenue DESC
LIMIT 20
```

### Recommended Charts

| Chart Type | Use Case |
|------------|----------|
| Time Series | Daily/hourly sales trends |
| Pie Chart | Payment method distribution |
| Bar Chart | Top products, store comparison |
| Big Number | KPIs (total sales, ticket count) |
| Table | Detailed transaction data |

## Health Check Endpoints

```bash
# Spring Boot Health
curl http://localhost:8081/actuator/health

# Prometheus Health
curl http://localhost:9090/-/healthy

# Superset Health
curl http://localhost:8088/health

# ClickHouse Ping
curl http://localhost:8123/ping
```

## Troubleshooting

### Common Issues

```bash
# Check Prometheus targets
curl http://localhost:9090/api/v1/targets

# Check Prometheus config
curl http://localhost:9090/api/v1/status/config

# Verify metrics endpoint
curl http://localhost:8081/actuator/prometheus | head -50

# Check Superset status
docker logs geo-retail-superset
```

### Log Analysis

```bash
# Prometheus logs
docker logs geo-retail-prometheus -f

# Superset logs
docker logs geo-retail-superset -f

# Spring Boot logs (filter by level)
docker logs geo-retail-backend 2>&1 | grep ERROR
```

## References

- Prometheus: https://prometheus.io/docs/
- Apache Superset: https://superset.apache.org/docs/
- Micrometer: https://micrometer.io/docs
- Spring Boot Actuator: https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html
- ClickHouse Driver for Superset: https://github.com/xzkostyan/clickhouse-sqlalchemy
