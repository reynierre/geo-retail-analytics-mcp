# Data Ingestion Guide

## Overview

Este documento describe cómo ingestar datos desde el sistema POS Geocom hacia ClickHouse para geo-retail-analytics.

## Data Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Geocom POS │────►│  Listener   │────►│   Kafka     │────►│  Consumer   │
│  (Source)   │     │  Service    │     │  Topic      │     │  Service    │
└─────────────┘     └──────┬──────┘     └─────────────┘     └──────┬──────┘
                           │                                        │
                           │                                        ▼
                           │                                 ┌─────────────┐
                           └────────────────────────────────►│ ClickHouse  │
                                    (HTTP API alternative)   │   (OLAP)    │
                                                             └─────────────┘
```

## Domain Model

### Legacy DTOs (Geocom)

```
InterfaceVtaDTO
├── InterfaceVtaDetDTO[]     (line items)
│   └── InterfaceVtaDtosDTO[] (discounts)
└── InterfaceVtaPagoDTO[]    (payments)
```

### New Domain (geo-retail-analytics)

```
Sale (Aggregate Root)
├── SaleItem[]
│   └── SaleItemDiscount[]
└── SalePayment[]
```

## Mapper Implementation

### SaleMapper.java

```java
package com.geocom.retail.infrastructure.mapper;

import com.geocom.retail.domain.sale.*;
import uy.com.geocom.geopos.services.exporter.sale.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicInteger;

public class SaleMapper {

    /**
     * Maps legacy Geocom DTO to new Sale domain model.
     */
    public static Sale fromLegacy(InterfaceVtaDTO dto) {
        return Sale.builder()
            // Keys
            .companyId(dto.getClientId())
            .storeId(dto.getOrgId())
            .posTerminalId(dto.getCodCaja())
            .ticketNumber(dto.getNroTicket())
            .saleDate(dto.getFechaTicket())

            // Audit
            .active(dto.getIsActive() == 'Y')
            .createdAt(dto.getCreated())
            .createdBy(dto.getCreatedBy())
            .updatedAt(dto.getUpdated())
            .updatedBy(dto.getUpdatedBy())

            // Transaction
            .cashierId(dto.getCodCajero())
            .currencyCode(dto.getIsoCode())

            // Amounts
            .subtotalAmount(toBigDecimal(dto.getAmtSubtotal()))
            .discountAmount(toBigDecimal(dto.getAmtDiscount()))
            .taxAmount(toBigDecimal(dto.getTaxAmt()))
            .roundingAmount(toBigDecimal(dto.getAmtRounding()))
            .surchargeAmount(toBigDecimal(dto.getAmtSurcharge()))
            .totalAmount(toBigDecimal(dto.getTotalAmt()))

            // Fiscal Document (CFE)
            .fiscalDocumentTypeId(dto.getTipoCfe())
            .fiscalDocumentSeries(dto.getSerieCfe())
            .fiscalDocumentNumber(dto.getNumeroCfe())

            // Customer
            .customerTaxTypeId(dto.getTaxGroupId())
            .customerTaxNumber(dto.getTaxId())

            // Related entities
            .items(mapItems(dto.getItemsDetails()))
            .payments(mapPayments(dto.getPayments()))
            .build();
    }

    private static List<SaleItem> mapItems(List<InterfaceVtaDetDTO> dtos) {
        if (dtos == null) return List.of();

        AtomicInteger lineNumber = new AtomicInteger(1);
        return dtos.stream()
            .map(dto -> mapItem(dto, lineNumber.getAndIncrement()))
            .collect(Collectors.toList());
    }

    private static SaleItem mapItem(InterfaceVtaDetDTO dto, int lineNumber) {
        return SaleItem.builder()
            .lineNumber(lineNumber)
            .active(dto.getIsActive() == 'Y')
            .productId(dto.getProductId())
            .barcode(dto.getUpc())
            .quantity(toBigDecimal(dto.getQtyEntered()))
            .grossUnitPrice(toBigDecimal(dto.getPrecioSinDto()))
            .discountAmount(toBigDecimal(dto.getAmtDiscount()))
            .netUnitPrice(toBigDecimal(dto.getPriceEntered()))
            .subtotalAmount(toBigDecimal(dto.getAmtSubtotal()))
            .taxAmount(toBigDecimal(dto.getTaxAmt()))
            .taxCode(dto.getCodigoIva())
            .totalAmount(toBigDecimal(dto.getTotalAmt()))
            .discounts(mapDiscounts(dto.getDiscounts()))
            .build();
    }

    private static List<SaleItemDiscount> mapDiscounts(List<InterfaceVtaDtosDTO> dtos) {
        if (dtos == null) return List.of();

        AtomicInteger seq = new AtomicInteger(1);
        return dtos.stream()
            .map(dto -> mapDiscount(dto, seq.getAndIncrement()))
            .collect(Collectors.toList());
    }

    private static SaleItemDiscount mapDiscount(InterfaceVtaDtosDTO dto, int sequence) {
        return SaleItemDiscount.builder()
            .discountSequence(sequence)
            .active(true)
            .appliedQuantity(dto.getQtyDto())
            .discountAmount(toBigDecimal(dto.getAmtDiscount()))
            .discountPercentage(toBigDecimal(dto.getPromotionPorc()))
            .promotionId(dto.getPromotionID())
            .promotionName(dto.getPromotionName())
            .promotionType(dto.getPromotionType())
            .promotionCategory(dto.getPromotionCategory())
            .campaignName(dto.getNomCampania())
            .financedDiscount(dto.getDtoFinanciado() != null && dto.getDtoFinanciado() == 'Y')
            .financedPercentage(toBigDecimal(dto.getPorcPromoFin()))
            .financedPromotionName(dto.getPromotionFinName())
            .promotionTaxId(dto.getPromotionTaxID())
            .agreementCode(dto.getCodConvenio())
            .build();
    }

    private static List<SalePayment> mapPayments(List<InterfaceVtaPagoDTO> dtos) {
        if (dtos == null) return List.of();

        AtomicInteger seq = new AtomicInteger(1);
        return dtos.stream()
            .map(dto -> mapPayment(dto, seq.getAndIncrement()))
            .collect(Collectors.toList());
    }

    private static SalePayment mapPayment(InterfaceVtaPagoDTO dto, int sequence) {
        return SalePayment.builder()
            .paymentSequence(sequence)
            .active(dto.getIsActive() == 'Y')
            .paymentMethodId(dto.getCodMedioPago())
            .currencyCode(dto.getIsoCode())
            .exchangeRate(toBigDecimal(dto.getConversionRate()))

            // Amounts
            .tenderedAmount(toBigDecimal(dto.getTotalEntregado()))
            .tenderedAmountLocal(toBigDecimal(dto.getTotalEntregadoMonRef()))
            .totalAmount(toBigDecimal(dto.getTotalAmt()))
            .discountAmount(toBigDecimal(dto.getAmtDiscount()))
            .surchargeAmount(toBigDecimal(dto.getAmountSurcharge()))
            .cashbackAmount(toBigDecimal(dto.getCashbackAmt()))
            .cashbackToAccountAmount(toBigDecimal(dto.getCashbackToAccountAmt()))

            // Flags
            .changePayment(dto.getEsCambio() == 'Y')
            .onlineTransaction(dto.getIsOnline() == 'Y')

            // Card
            .cardProductId(dto.getCodTipoTarjeta())
            .maskedCardNumber(dto.getNroTarjeta())
            .cardType(dto.getTipoTarjeta())
            .cardBrandId(dto.getBrandID())
            .cardBrandName(dto.getBrandName())
            .cardProductCode(dto.getCodProdTarjeta())
            .cardProductName(dto.getNomProdTarjeta())
            .cardEntryMode(dto.getModoIngTarjeta())

            // Authorization
            .authorizationCode(dto.getCodAutorizacion())
            .terminalId(dto.getCodTerminal())
            .merchantId(dto.getCodComercio())
            .batchId(dto.getPosBatchID())
            .voucherNumber(dto.getNroVoucher())
            .originalVoucherNumber(dto.getNroVoucherOrigl())
            .originalVoucherDate(dto.getDateVoucherOrig())

            // VAT Discount
            .vatDiscountApplied(dto.getAplicaDtoIVA() == 'Y')
            .taxLawCode(dto.getCodLeyImpuesto())

            // Plan
            .paymentPlanId(dto.getPlanID())
            .installmentCount(dto.getCantCuotas() != null ? dto.getCantCuotas() : 1)

            // Check
            .checkNumber(dto.getCheckNumber())
            .checkBankCode(dto.getCheckBankNumber())
            .checkExpirationDate(dto.getCheckValidDate())

            // Account
            .accountType(dto.getAccountType())
            .accountNumber(dto.getAccountNumber())
            .accountAuthCode(dto.getAccountAuthorizationCode())
            .build();
    }

    private static BigDecimal toBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : BigDecimal.ZERO;
    }

    private static BigDecimal toBigDecimal(Integer value) {
        return value != null ? BigDecimal.valueOf(value) : BigDecimal.ZERO;
    }
}
```

## Ingestion Methods

### Method 1: Kafka Streaming (Recommended)

Best for real-time ingestion with high throughput.

```java
package com.geocom.retail.infrastructure.adapter.out.kafka;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaSaleExporter implements SaleExporter {

    private final KafkaTemplate<String, Sale> kafkaTemplate;
    private final SaleMapper saleMapper;

    @Override
    public void export(InterfaceVtaDTO vtaDTO) {
        Sale sale = SaleMapper.fromLegacy(vtaDTO);

        String key = buildKey(sale);

        kafkaTemplate.send("sales-events", key, sale)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send sale {}: {}", key, ex.getMessage());
                    // Implement retry or dead letter queue
                } else {
                    log.debug("Sale {} sent to Kafka, partition {}",
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

### Method 2: HTTP API

For lower volume or when Kafka is not available.

```java
package com.geocom.retail.infrastructure.adapter.out.http;

@Service
@RequiredArgsConstructor
@Slf4j
public class HttpSaleExporter implements SaleExporter {

    private final RestTemplate restTemplate;

    @Value("${analytics.api.url}")
    private String apiUrl;

    @Override
    public void export(InterfaceVtaDTO vtaDTO) {
        Sale sale = SaleMapper.fromLegacy(vtaDTO);

        try {
            restTemplate.postForEntity(
                apiUrl + "/api/ingest/sale",
                sale,
                Void.class
            );
            log.debug("Sale {} sent via HTTP", sale.getTicketNumber());
        } catch (RestClientException e) {
            log.error("Failed to send sale {}: {}",
                sale.getTicketNumber(), e.getMessage());
            throw e;
        }
    }
}
```

### Method 3: Batch (Historical Migration)

For bulk loading historical data.

```java
package com.geocom.retail.infrastructure.adapter.out.batch;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchSaleExporter {

    private final ClickHouseSaleRepository repository;

    @Transactional
    public void exportBatch(List<InterfaceVtaDTO> vtaDTOs) {
        List<Sale> sales = vtaDTOs.stream()
            .map(SaleMapper::fromLegacy)
            .collect(Collectors.toList());

        // Insert in batches of 10000
        Lists.partition(sales, 10000).forEach(batch -> {
            repository.insertSalesBatch(batch);
            log.info("Inserted batch of {} sales", batch.size());
        });

        log.info("Exported {} sales in batch", sales.size());
    }
}
```

## ClickHouse Repository

```java
package com.geocom.retail.infrastructure.adapter.out.persistence;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ClickHouseSaleRepository {

    private final JdbcTemplate jdbcTemplate;

    public void insertSale(Sale sale) {
        String sql = """
            INSERT INTO fact_sales (
                company_id, store_id, pos_terminal_id, ticket_number, sale_date,
                active, created_at, created_by, updated_at, updated_by,
                cashier_id, currency_code,
                subtotal_amount, discount_amount, tax_amount, rounding_amount,
                surcharge_amount, total_amount,
                fiscal_document_type_id, fiscal_document_series, fiscal_document_number,
                customer_tax_type_id, customer_tax_number
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        jdbcTemplate.update(sql,
            sale.getCompanyId(),
            sale.getStoreId(),
            sale.getPosTerminalId(),
            sale.getTicketNumber(),
            sale.getSaleDate(),
            sale.isActive() ? 1 : 0,
            sale.getCreatedAt(),
            sale.getCreatedBy(),
            sale.getUpdatedAt(),
            sale.getUpdatedBy(),
            sale.getCashierId(),
            sale.getCurrencyCode(),
            sale.getSubtotalAmount(),
            sale.getDiscountAmount(),
            sale.getTaxAmount(),
            sale.getRoundingAmount(),
            sale.getSurchargeAmount(),
            sale.getTotalAmount(),
            sale.getFiscalDocumentTypeId(),
            sale.getFiscalDocumentSeries(),
            sale.getFiscalDocumentNumber(),
            sale.getCustomerTaxTypeId(),
            sale.getCustomerTaxNumber()
        );
    }

    public void insertSaleItems(Long companyId, Long storeId, String posTerminalId,
                                 String ticketNumber, LocalDateTime saleDate,
                                 List<SaleItem> items) {
        String sql = """
            INSERT INTO fact_sale_items (
                company_id, store_id, pos_terminal_id, ticket_number, sale_date,
                line_number, active, product_id, barcode, quantity,
                gross_unit_price, discount_amount, net_unit_price,
                subtotal_amount, tax_amount, tax_code, total_amount
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                SaleItem item = items.get(i);
                ps.setLong(1, companyId);
                ps.setLong(2, storeId);
                ps.setString(3, posTerminalId);
                ps.setString(4, ticketNumber);
                ps.setTimestamp(5, Timestamp.valueOf(saleDate));
                ps.setInt(6, item.getLineNumber());
                ps.setInt(7, item.isActive() ? 1 : 0);
                ps.setLong(8, item.getProductId());
                ps.setString(9, item.getBarcode());
                ps.setBigDecimal(10, item.getQuantity());
                ps.setBigDecimal(11, item.getGrossUnitPrice());
                ps.setBigDecimal(12, item.getDiscountAmount());
                ps.setBigDecimal(13, item.getNetUnitPrice());
                ps.setBigDecimal(14, item.getSubtotalAmount());
                ps.setBigDecimal(15, item.getTaxAmount());
                ps.setString(16, item.getTaxCode());
                ps.setBigDecimal(17, item.getTotalAmount());
            }

            @Override
            public int getBatchSize() {
                return items.size();
            }
        });
    }

    // Similar methods for payments and discounts...
}
```

## Historical Data Migration

### Step 1: Export from Legacy Database

```sql
-- From your legacy Oracle/PostgreSQL database
SELECT * FROM ventas
WHERE fecha >= '2024-01-01'
ORDER BY fecha;
```

### Step 2: Transform and Load

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class MigrationService {

    private final LegacyRepository legacyRepository;
    private final ClickHouseSaleRepository clickHouseRepository;

    public void migrateHistoricalData(LocalDate startDate, LocalDate endDate) {
        log.info("Starting migration from {} to {}", startDate, endDate);

        // Process day by day to avoid memory issues
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            List<InterfaceVtaDTO> dayData = legacyRepository.getSalesForDate(current);

            List<Sale> sales = dayData.stream()
                .map(SaleMapper::fromLegacy)
                .collect(Collectors.toList());

            // Insert in batches
            Lists.partition(sales, 10000).forEach(batch -> {
                clickHouseRepository.insertSalesBatch(batch);
            });

            log.info("Migrated {} sales for {}", dayData.size(), current);
            current = current.plusDays(1);
        }

        log.info("Migration completed");
    }
}
```

### Step 3: Direct CSV Load (Alternative)

```bash
# Export to CSV from legacy system
# Then load directly to ClickHouse

docker exec -it geo-retail-clickhouse clickhouse-client --query="
INSERT INTO fact_sales FORMAT CSVWithNames
" < sales_export.csv
```

## Testing

### Integration Test

```java
@SpringBootTest
@Testcontainers
class SaleIngestionTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Container
    static ClickHouseContainer clickhouse = new ClickHouseContainer(
        DockerImageName.parse("clickhouse/clickhouse-server:24-alpine"));

    @Autowired
    private KafkaSaleExporter kafkaSaleExporter;

    @Autowired
    private ClickHouseSaleRepository repository;

    @Test
    void shouldIngestSaleViaKafka() {
        // Given
        InterfaceVtaDTO legacyDTO = createTestDTO();

        // When
        kafkaSaleExporter.export(legacyDTO);

        // Then
        await().atMost(10, SECONDS).untilAsserted(() -> {
            Long count = repository.countSales();
            assertThat(count).isEqualTo(1);
        });
    }

    private InterfaceVtaDTO createTestDTO() {
        InterfaceVtaDTO dto = new InterfaceVtaDTO();
        dto.setClientId(1L);
        dto.setOrgId(1L);
        dto.setCodCaja("01");
        dto.setNroTicket("TEST001");
        dto.setFechaTicket(LocalDateTime.now());
        dto.setTotalAmt(1500.0);
        // ... set other fields
        return dto;
    }
}
```

## Monitoring Ingestion

### Key Metrics

```promql
# Sales ingested per minute
rate(sales_ingested_total[1m])

# Ingestion errors
rate(sales_ingestion_errors_total[5m])

# Kafka consumer lag
kafka_consumer_lag{group="geo-retail-analytics"}

# ClickHouse insert rate
rate(ClickHouseProfileEvents_InsertedRows[5m])
```

### Grafana Dashboard

Create a dashboard with:
- Sales ingested per minute
- Ingestion error rate
- Kafka consumer lag
- ClickHouse insert latency
- Daily sales count

## Troubleshooting

### Common Issues

1. **Kafka consumer lag increasing**
   - Check consumer logs
   - Increase consumer instances
   - Optimize ClickHouse inserts

2. **ClickHouse insert errors**
   - Check data types match schema
   - Verify nullable fields
   - Check disk space

3. **Duplicate sales**
   - Implement idempotent consumers
   - Use ReplacingMergeTree

### Debug Commands

```bash
# Check Kafka topic
docker exec geo-retail-kafka kafka-console-consumer \
  --topic sales-events \
  --bootstrap-server localhost:9092 \
  --from-beginning --max-messages 10

# Check ClickHouse inserts
docker exec geo-retail-clickhouse clickhouse-client --query="
SELECT toDate(sale_date), count()
FROM fact_sales
GROUP BY toDate(sale_date)
ORDER BY 1 DESC
LIMIT 10"

# Check for errors in logs
docker logs geo-retail-backend 2>&1 | grep -i error | tail -50
```

## References

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [ClickHouse Insert Guide](https://clickhouse.com/docs/en/sql-reference/statements/insert-into)
- [Spring Kafka Reference](https://docs.spring.io/spring-kafka/docs/current/reference/html/)
