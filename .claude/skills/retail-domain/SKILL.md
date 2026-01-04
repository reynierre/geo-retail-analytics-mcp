# Retail Domain Skill

## Overview

This skill provides domain knowledge for retail analytics based on **Geocom POS** data model. The core domain is **Sale** which represents a complete retail transaction with items, discounts, and payments.

## Sale Domain (Geocom POS)

### Domain Structure

```
Sale (Aggregate Root)
├── companyId, storeId, posTerminalId, ticketNumber
├── saleDate, cashierId
├── amounts: subtotal, discount, tax, rounding, surcharge, total
├── fiscalDocument: typeId, series, number (CFE Uruguay)
├── customer: taxTypeId, taxNumber
│
├── SaleItem[] (lineas de venta)
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
    ├── tenderedAmount, totalAmount, discountAmount
    ├── Card: brandId, maskedNumber, type, entryMode
    ├── Plan: paymentPlanId, installmentCount
    ├── VAT: vatDiscountApplied, taxLawCode
    └── Check/Account: number, bankCode
```

### Sale (Aggregate Root)

Principal entity representing a retail transaction. Source: `InterfaceVtaDTO`

| Field | Type | Description |
|-------|------|-------------|
| companyId | Long | Company ID |
| storeId | Long | Store/branch ID |
| posTerminalId | String | POS terminal ID |
| ticketNumber | String | Ticket number |
| saleDate | Timestamp | Sale date/time |
| cashierId | String | Cashier ID |
| currencyCode | String | Currency (UYU) |
| subtotalAmount | BigDecimal | Subtotal before discounts |
| discountAmount | BigDecimal | Total discounts |
| taxAmount | BigDecimal | Total taxes |
| roundingAmount | BigDecimal | Rounding adjustment |
| surchargeAmount | BigDecimal | Surcharges |
| totalAmount | BigDecimal | Final total |
| fiscalDocumentTypeId | String | CFE type |
| fiscalDocumentSeries | String | CFE series |
| fiscalDocumentNumber | String | CFE number |
| customerTaxTypeId | Integer | 0=None, 1000000=RUT, 1000001=CI |
| customerTaxNumber | String | Customer RUT/CI |

### SaleItem

Line item within a sale. Source: `InterfaceVtaDetDTO`

| Field | Type | Description |
|-------|------|-------------|
| lineNumber | Integer | Line sequence |
| productId | Long | Product ID |
| barcode | String | Product barcode |
| quantity | BigDecimal | Quantity sold |
| grossUnitPrice | BigDecimal | Price before discount |
| netUnitPrice | BigDecimal | Price after discount |
| discountAmount | BigDecimal | Line discount |
| subtotalAmount | BigDecimal | Net * quantity |
| taxAmount | BigDecimal | Line tax |
| taxCode | String | Tax code (IVA) |
| totalAmount | BigDecimal | Subtotal + tax |

### SaleItemDiscount

Discount applied to a line item. Source: `InterfaceVtaDtosDTO`

| Field | Type | Description |
|-------|------|-------------|
| appliedQuantity | Integer | Units with discount |
| discountAmount | BigDecimal | Discount amount |
| discountPercentage | BigDecimal | Discount % |
| promotionId | String | Promotion ID |
| promotionName | String | Promotion name |
| promotionType | String | Type (%, fixed, etc) |
| promotionCategory | String | Category |
| campaignName | String | Campaign name |
| financedDiscount | boolean | Bank-financed |
| financedPercentage | BigDecimal | Financed % |
| agreementCode | Integer | Bank agreement code |

### SalePayment

Payment for a sale. Source: `InterfaceVtaPagoDTO`

| Field | Type | Description |
|-------|------|-------------|
| paymentSequence | Integer | Payment order |
| paymentMethodId | String | Method (EF, TC, TD) |
| currencyCode | String | Currency |
| exchangeRate | BigDecimal | FX rate |
| tenderedAmount | BigDecimal | Amount tendered |
| totalAmount | BigDecimal | Payment amount |
| discountAmount | BigDecimal | Payment discount |
| surchargeAmount | BigDecimal | Surcharge |
| changePayment | boolean | Is change |
| cardBrandId | String | Card brand |
| cardBrandName | String | Brand name |
| cardType | Character | C=Credit, D=Debit |
| maskedCardNumber | String | Masked PAN |
| cardEntryMode | Character | C=Chip, M=Mag, F=NFC |
| authorizationCode | String | Auth code |
| installmentCount | Integer | Cuotas/installments |
| vatDiscountApplied | boolean | IVA discount |
| taxLawCode | String | Tax law code |

## Key Retail Metrics

### Sales Metrics

| Metric | Description | Formula |
|--------|-------------|---------|
| **Total Sales** | Sum of all sales | `SUM(total_amount)` |
| **Ticket Count** | Number of transactions | `COUNT(*)` |
| **Average Ticket** | Average transaction value | `AVG(total_amount)` |
| **Discount Rate** | Discount percentage | `SUM(discount) / SUM(subtotal) * 100` |

### Payment Metrics

| Metric | Description | Formula |
|--------|-------------|---------|
| **Cash vs Card** | Payment mix | `SUM by payment_method` |
| **Card Brand Share** | Brand distribution | `SUM by card_brand` |
| **Avg Installments** | Average cuotas | `AVG(installment_count)` |
| **VAT Discount Impact** | IVA law benefit | `SUM(discount WHERE vat_applied)` |

### Product Metrics

| Metric | Description | Formula |
|--------|-------------|---------|
| **Units Sold** | Total quantity | `SUM(quantity)` |
| **Revenue** | Product revenue | `SUM(total_amount)` |
| **Discount Impact** | Discounts given | `SUM(discount_amount)` |

## Common Queries (New Schema)

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

### Promotion Analysis

```sql
SELECT
    d.promotion_id,
    d.promotion_name,
    d.promotion_type,
    count() AS times_applied,
    sum(d.applied_quantity) AS units_discounted,
    sum(d.discount_amount) AS total_discount,
    sum(if(d.financed_discount, d.discount_amount, 0)) AS financed_amount
FROM fact_sale_item_discounts d
WHERE d.company_id = {company_id}
  AND d.promotion_id IS NOT NULL
  AND d.active = 1
GROUP BY d.promotion_id, d.promotion_name, d.promotion_type
ORDER BY total_discount DESC;
```

## Business Rules

### Date Interpretations

| User Says | Interpret As |
|-----------|--------------|
| "hoy" | Current date |
| "ayer" | Yesterday |
| "esta semana" | Monday to today |
| "este mes" | 1st of month to today |
| "mes pasado" | Full previous month |

### Number Formatting (Uruguay)

```
Thousands: 1.234.567 (dot separator)
Decimals: 1.234,56 (comma separator)
Currency: $1.234.567 (UYU)
Percentage: 15,5%
```

### Payment Method Codes

| Code | Description |
|------|-------------|
| EF | Efectivo (Cash) |
| TC | Tarjeta Credito (Credit Card) |
| TD | Tarjeta Debito (Debit Card) |
| CH | Cheque |
| TR | Transferencia |
| CC | Cuenta Corriente (Account) |

### Card Types

| Code | Description |
|------|-------------|
| C | Credit |
| D | Debit |

### Card Entry Modes

| Code | Description |
|------|-------------|
| C | Chip (EMV) |
| M | Magnetic stripe |
| F | Contactless (NFC) |
| K | Keyed/Manual |

### Tax Types (Uruguay)

| ID | Description |
|----|-------------|
| 0 | None/Anonymous |
| 1000000 | RUT (Empresa) |
| 1000001 | CI (Cedula) |
| 1000002 | Other |

## Mapping: Geocom DTO -> Sale Domain

### InterfaceVtaDTO -> Sale

| Original | New Field |
|----------|-----------|
| clientId | companyId |
| orgId | storeId |
| codCaja | posTerminalId |
| nroTicket | ticketNumber |
| fechaTicket | saleDate |
| codCajero | cashierId |
| amtSubtotal | subtotalAmount |
| amtDiscount | discountAmount |
| totalAmt | totalAmount |
| tipoCfe | fiscalDocumentTypeId |

### InterfaceVtaDetDTO -> SaleItem

| Original | New Field |
|----------|-----------|
| upc | barcode |
| qtyEntered | quantity |
| precioSinDto | grossUnitPrice |
| priceEntered | netUnitPrice |
| codigoIva | taxCode |

### InterfaceVtaPagoDTO -> SalePayment

| Original | New Field |
|----------|-----------|
| codMedioPago | paymentMethodId |
| nroTarjeta | maskedCardNumber |
| totalEntregado | tenderedAmount |
| cantCuotas | installmentCount |
| aplicaDtoIVA | vatDiscountApplied |
| modoIngTarjeta | cardEntryMode |

## References

- Geocom POS Integration
- Uruguay CFE (Comprobante Fiscal Electronico)
- IVA Discount Law (Uruguay)
