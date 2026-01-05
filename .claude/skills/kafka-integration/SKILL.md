# Kafka Integration Skill

## Overview

Integración de Apache Kafka para streaming de eventos de ventas en tiempo real desde el sistema POS Geocom hacia ClickHouse.

## Architecture

```
Geocom POS ──► Listener ──► Sale DTOs ──► Kafka ──► Consumer ──► ClickHouse
                                │
                                └──► HTTP API (alternativo)
```

## Topics

### sales-events

Topic principal para eventos de venta.

```json
{
  "topic": "sales-events",
  "partitions": 6,
  "replication_factor": 1,
  "config": {
    "retention.ms": 604800000,
    "cleanup.policy": "delete"
  }
}
```

### Sale Event Schema

```json
{
  "companyId": 1,
  "storeId": 5,
  "posTerminalId": "01",
  "ticketNumber": "123456",
  "saleDate": "2025-01-03T10:30:00Z",
  "cashierId": "001",
  "currencyCode": "UYU",
  "subtotalAmount": 1500.00,
  "discountAmount": 150.00,
  "taxAmount": 297.00,
  "totalAmount": 1647.00,
  "fiscalDocumentTypeId": "111",
  "fiscalDocumentSeries": "A",
  "fiscalDocumentNumber": "1234567",
  "items": [
    {
      "lineNumber": 1,
      "productId": 12345,
      "barcode": "7890123456789",
      "quantity": 2.000,
      "grossUnitPrice": 500.00,
      "netUnitPrice": 450.00,
      "discountAmount": 100.00,
      "totalAmount": 900.00
    }
  ],
  "payments": [
    {
      "paymentSequence": 1,
      "paymentMethodId": "TC",
      "totalAmount": 1647.00,
      "cardBrandId": "VISA",
      "installmentCount": 3
    }
  ]
}
```

## Spring Kafka Configuration

### Gradle Dependencies

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springframework.kafka:spring-kafka-test")
}
```

### Application Configuration

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: geo-retail-analytics
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "uy.com.geocom.retail.domain.sale"
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

## Consumer Implementation

```java
package uy.com.geocom.retail.infrastructure.adapter.in.kafka;

@Component
@RequiredArgsConstructor
@Slf4j
public class SaleEventConsumer {

    private final ClickHouseSaleRepository repository;
    private final SalesMetrics metrics;

    @KafkaListener(topics = "sales-events", groupId = "geo-retail-analytics")
    public void consumeSaleEvent(Sale sale) {
        log.info("Received sale event: company={}, store={}, ticket={}",
            sale.getCompanyId(),
            sale.getStoreId(),
            sale.getTicketNumber());

        try {
            repository.insertSale(sale);
            repository.insertSaleItems(sale.getItems());
            repository.insertSalePayments(sale.getPayments());

            metrics.recordSale(sale);
            log.debug("Sale {} persisted successfully", sale.getTicketNumber());
        } catch (Exception e) {
            log.error("Error persisting sale {}: {}",
                sale.getTicketNumber(), e.getMessage());
            throw e; // Retry via Kafka
        }
    }
}
```

## Producer Implementation

```java
package uy.com.geocom.retail.infrastructure.adapter.out.kafka;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaleEventProducer {

    private final KafkaTemplate<String, Sale> kafkaTemplate;

    public void sendSaleEvent(Sale sale) {
        String key = buildKey(sale);

        kafkaTemplate.send("sales-events", key, sale)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send sale event {}: {}", key, ex.getMessage());
                } else {
                    log.info("Sale event sent: {} -> partition {}",
                        key, result.getRecordMetadata().partition());
                }
            });
    }

    private String buildKey(Sale sale) {
        return String.format("%d-%d-%s-%s",
            sale.getCompanyId(),
            sale.getStoreId(),
            sale.getPosTerminalId(),
            sale.getTicketNumber());
    }
}
```

## Kafka CLI Commands

```bash
# Create topic
docker exec -it geo-retail-kafka kafka-topics \
  --create \
  --topic sales-events \
  --bootstrap-server localhost:9092 \
  --partitions 6 \
  --replication-factor 1

# List topics
docker exec -it geo-retail-kafka kafka-topics \
  --list \
  --bootstrap-server localhost:9092

# Describe topic
docker exec -it geo-retail-kafka kafka-topics \
  --describe \
  --topic sales-events \
  --bootstrap-server localhost:9092

# Consume messages (debug)
docker exec -it geo-retail-kafka kafka-console-consumer \
  --topic sales-events \
  --bootstrap-server localhost:9092 \
  --from-beginning

# Producer test
docker exec -it geo-retail-kafka kafka-console-producer \
  --topic sales-events \
  --bootstrap-server localhost:9092

# Consumer groups
docker exec -it geo-retail-kafka kafka-consumer-groups \
  --list \
  --bootstrap-server localhost:9092

# Check consumer lag
docker exec -it geo-retail-kafka kafka-consumer-groups \
  --describe \
  --group geo-retail-analytics \
  --bootstrap-server localhost:9092

# Reset offset
docker exec -it geo-retail-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group geo-retail-analytics \
  --topic sales-events \
  --reset-offsets --to-earliest --execute
```

## Error Handling

### Dead Letter Queue

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, Sale> kafkaListenerContainerFactory(
        ConsumerFactory<String, Sale> consumerFactory,
        KafkaTemplate<String, Sale> template) {

    ConcurrentKafkaListenerContainerFactory<String, Sale> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);

    // Configure error handler with DLQ
    DefaultErrorHandler errorHandler = new DefaultErrorHandler(
        new DeadLetterPublishingRecoverer(template),
        new FixedBackOff(1000L, 3L)); // 3 retries, 1 second apart

    factory.setCommonErrorHandler(errorHandler);
    return factory;
}
```

## Monitoring

- **Kafka UI**: http://localhost:8080
- **Consumer Lag**: Check via Grafana dashboard
- **Throughput**: Prometheus metrics

### Key Metrics

```promql
# Messages per second
rate(kafka_server_brokertopicmetrics_messagesin_total[5m])

# Consumer lag
kafka_consumer_lag

# Partition count
kafka_server_replicamanager_partitioncount
```

## References

- Apache Kafka: https://kafka.apache.org/documentation/
- Spring Kafka: https://docs.spring.io/spring-kafka/docs/current/reference/html/
- Confluent Kafka: https://docs.confluent.io/platform/current/
