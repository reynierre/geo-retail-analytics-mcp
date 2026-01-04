-- =====================================================
-- Geo Retail Analytics - ClickHouse Schema
-- Domain: Sale (based on Geocom POS)
-- FIXED: alias conflicts + nullable in ORDER BY
-- =====================================================

CREATE DATABASE IF NOT EXISTS geo_retail_analytics;
USE geo_retail_analytics;

-- =====================================================
-- FACT: Sales (Aggregate Root)
-- =====================================================
CREATE TABLE IF NOT EXISTS fact_sales (
                                          company_id UInt64,
                                          store_id UInt64,
                                          pos_terminal_id LowCardinality(String),
    ticket_number String,
    sale_date DateTime,

    active UInt8 DEFAULT 1,
    created_at DateTime DEFAULT now(),
    created_by UInt32,
    updated_at DateTime DEFAULT now(),
    updated_by UInt32,

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
    TTL sale_date + INTERVAL 5 YEAR DELETE
SETTINGS index_granularity = 8192;

-- =====================================================
-- FACT: Sale Items
-- =====================================================
CREATE TABLE IF NOT EXISTS fact_sale_items (
                                               company_id UInt64,
                                               store_id UInt64,
                                               pos_terminal_id LowCardinality(String),
    ticket_number String,
    sale_date DateTime,

    line_number UInt16,
    active UInt8 DEFAULT 1,

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

-- =====================================================
-- FACT: Sale Item Discounts
-- =====================================================
CREATE TABLE IF NOT EXISTS fact_sale_item_discounts (
                                                        company_id UInt64,
                                                        store_id UInt64,
                                                        pos_terminal_id LowCardinality(String),
    ticket_number String,
    sale_date DateTime,
    line_number UInt16,

    discount_sequence UInt8,
    active UInt8 DEFAULT 1,
    applied_quantity UInt32,
    discount_amount Decimal(12,2),
    discount_percentage Nullable(Decimal(5,2)),

    promotion_id Nullable(String),
    promotion_name Nullable(String),
    promotion_type LowCardinality(Nullable(String)),
    promotion_category LowCardinality(Nullable(String)),
    campaign_name Nullable(String),

    financed_discount UInt8 DEFAULT 0,
    financed_percentage Nullable(Decimal(5,2)),
    financed_promotion_name Nullable(String),
    promotion_tax_id Nullable(String),
    agreement_code Nullable(UInt32)
    )
    ENGINE = MergeTree()
    PARTITION BY toYYYYMM(sale_date)
    ORDER BY (company_id, store_id, sale_date, pos_terminal_id, ticket_number, line_number, discount_sequence);

-- =====================================================
-- FACT: Sale Payments
-- =====================================================
CREATE TABLE IF NOT EXISTS fact_sale_payments (
                                                  company_id UInt64,
                                                  store_id UInt64,
                                                  pos_terminal_id LowCardinality(String),
    ticket_number String,
    sale_date DateTime,

    payment_sequence UInt8,
    active UInt8 DEFAULT 1,
    payment_method_id LowCardinality(String),

    currency_code LowCardinality(String) DEFAULT 'UYU',
    exchange_rate Nullable(Decimal(12,6)),

    tendered_amount Decimal(12,2),
    tendered_amount_local Decimal(12,2),
    total_amount Decimal(12,2),
    discount_amount Decimal(12,2) DEFAULT 0,
    surcharge_amount Decimal(12,2) DEFAULT 0,
    cashback_amount Decimal(12,2) DEFAULT 0,
    cashback_to_account_amount Decimal(12,2) DEFAULT 0,

    change_payment UInt8 DEFAULT 0,
    online_transaction UInt8 DEFAULT 1,

    card_product_id Nullable(String),
    masked_card_number Nullable(String),
    card_type Nullable(FixedString(1)),
    card_brand_id Nullable(String),
    card_brand_name LowCardinality(Nullable(String)),
    card_product_code Nullable(String),
    card_product_name Nullable(String),
    card_entry_mode Nullable(FixedString(1)),

    authorization_code Nullable(String),
    terminal_id Nullable(String),
    merchant_id Nullable(String),
    batch_id Nullable(UInt64),
    voucher_number Nullable(String),

    original_voucher_number Nullable(String),
    original_voucher_date Nullable(DateTime),

    vat_discount_applied UInt8 DEFAULT 0,
    tax_law_code Nullable(String),

    payment_plan_id Nullable(String),
    installment_count UInt8 DEFAULT 1,

    check_number Nullable(String),
    check_bank_code Nullable(String),
    check_expiration_date Nullable(Date),

    account_type Nullable(String),
    account_number Nullable(String),
    account_auth_code Nullable(String)
    )
    ENGINE = MergeTree()
    PARTITION BY toYYYYMM(sale_date)
    ORDER BY (company_id, store_id, sale_date, pos_terminal_id, ticket_number, payment_sequence);

-- =====================================================
-- DIMENSIONS
-- =====================================================

CREATE TABLE IF NOT EXISTS dim_companies (
                                             company_id UInt64,
                                             company_name String,
                                             tax_number String,
                                             country_code LowCardinality(String) DEFAULT 'UY',
    active UInt8 DEFAULT 1,
    updated_at DateTime DEFAULT now()
    )
    ENGINE = ReplacingMergeTree(updated_at)
    ORDER BY company_id;

CREATE TABLE IF NOT EXISTS dim_stores (
                                          store_id UInt64,
                                          company_id UInt64,
                                          store_code String,
                                          store_name String,
                                          region LowCardinality(String),
    city String,
    address Nullable(String),
    store_format LowCardinality(String),
    active UInt8 DEFAULT 1,
    updated_at DateTime DEFAULT now()
    )
    ENGINE = ReplacingMergeTree(updated_at)
    ORDER BY (company_id, store_id);

CREATE TABLE IF NOT EXISTS dim_products (
                                            product_id UInt64,
                                            company_id UInt64,
                                            barcode String,
                                            product_name String,
                                            category LowCardinality(String),
    subcategory LowCardinality(String),
    brand LowCardinality(String),
    unit_of_measure LowCardinality(String),
    active UInt8 DEFAULT 1,
    updated_at DateTime DEFAULT now()
    )
    ENGINE = ReplacingMergeTree(updated_at)
    ORDER BY (company_id, product_id);

CREATE TABLE IF NOT EXISTS dim_payment_methods (
                                                   payment_method_id String,
                                                   payment_method_name String,
                                                   payment_type LowCardinality(String),
    active UInt8 DEFAULT 1,
    updated_at DateTime DEFAULT now()
    )
    ENGINE = ReplacingMergeTree(updated_at)
    ORDER BY payment_method_id;

CREATE TABLE IF NOT EXISTS dim_taxes (
                                         tax_code String,
                                         tax_name String,
                                         tax_rate Decimal(5,2),
    tax_type LowCardinality(String),
    country_code LowCardinality(String) DEFAULT 'UY',
    active UInt8 DEFAULT 1,
    updated_at DateTime DEFAULT now()
    )
    ENGINE = ReplacingMergeTree(updated_at)
    ORDER BY tax_code;

CREATE TABLE IF NOT EXISTS dim_card_brands (
                                               card_brand_id String,
                                               card_brand_name String,
                                               card_network LowCardinality(String),
    active UInt8 DEFAULT 1,
    updated_at DateTime DEFAULT now()
    )
    ENGINE = ReplacingMergeTree(updated_at)
    ORDER BY card_brand_id;

CREATE TABLE IF NOT EXISTS dim_promotions (
                                              promotion_id String,
                                              company_id UInt64,
                                              promotion_name String,
                                              promotion_type LowCardinality(String),
    promotion_category LowCardinality(String),
    campaign_name Nullable(String),
    start_date Date,
    end_date Nullable(Date),
    financed_discount UInt8 DEFAULT 0,
    active UInt8 DEFAULT 1,
    updated_at DateTime DEFAULT now()
    )
    ENGINE = ReplacingMergeTree(updated_at)
    ORDER BY (company_id, promotion_id);

-- =====================================================
-- MATERIALIZED VIEWS (FIXED)
-- =====================================================

-- Daily Sales Summary
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_daily_sales
ENGINE = SummingMergeTree()
PARTITION BY toYYYYMM(the_date)
ORDER BY (company_id, store_id, the_date)
AS SELECT
              company_id,
              store_id,
              toDate(sale_date) AS the_date,
              count() AS ticket_count,
              sum(subtotal_amount) AS total_subtotal,
              sum(discount_amount) AS total_discounts,
              sum(tax_amount) AS total_taxes,
              sum(total_amount) AS total_sales
   FROM fact_sales
   WHERE active = 1
   GROUP BY company_id, store_id, the_date;

-- Hourly Sales
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_hourly_sales
ENGINE = SummingMergeTree()
ORDER BY (company_id, store_id, the_date, the_hour)
AS SELECT
              company_id,
              store_id,
              toDate(sale_date) AS the_date,
              toHour(sale_date) AS the_hour,
              count() AS ticket_count,
              sum(total_amount) AS total_sales
   FROM fact_sales
   WHERE active = 1
   GROUP BY company_id, store_id, the_date, the_hour;

-- Payment Summary
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_payment_summary
ENGINE = SummingMergeTree()
PARTITION BY toYYYYMM(the_date)
ORDER BY (company_id, store_id, the_date, payment_method_id)
AS SELECT
              company_id,
              store_id,
              toDate(sale_date) AS the_date,
              payment_method_id,
              count() AS payment_count,
              sum(total_amount) AS total_amount,
              sum(discount_amount) AS total_discounts,
              sum(surcharge_amount) AS total_surcharges
   FROM fact_sale_payments
   WHERE active = 1
   GROUP BY company_id, store_id, the_date, payment_method_id;

-- Product Sales
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_product_sales
ENGINE = SummingMergeTree()
PARTITION BY toYYYYMM(the_date)
ORDER BY (company_id, store_id, the_date, product_id)
AS SELECT
              company_id,
              store_id,
              toDate(sale_date) AS the_date,
              product_id,
              count() AS times_sold,
              sum(quantity) AS quantity_sold,
              sum(total_amount) AS total_revenue,
              sum(discount_amount) AS total_discounts
   FROM fact_sale_items
   WHERE active = 1
   GROUP BY company_id, store_id, the_date, product_id;

-- Promotion Summary (FIXED: coalesce to avoid nullable in ORDER BY)
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_promotion_summary
ENGINE = SummingMergeTree()
PARTITION BY toYYYYMM(the_date)
ORDER BY (company_id, store_id, the_date, promo_id, promo_type)
AS SELECT
              company_id,
              store_id,
              toDate(sale_date) AS the_date,
              coalesce(promotion_id, '') AS promo_id,
              coalesce(promotion_type, '') AS promo_type,
              count() AS times_applied,
              sum(applied_quantity) AS total_units,
              sum(discount_amount) AS total_discount_amount
   FROM fact_sale_item_discounts
   WHERE active = 1 AND promotion_id IS NOT NULL
   GROUP BY company_id, store_id, the_date, promo_id, promo_type;

-- Card Brand Summary (FIXED: coalesce to avoid nullable in ORDER BY)
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_card_brand_summary
ENGINE = SummingMergeTree()
PARTITION BY toYYYYMM(the_date)
ORDER BY (company_id, store_id, the_date, brand_id)
AS SELECT
              company_id,
              store_id,
              toDate(sale_date) AS the_date,
              coalesce(card_brand_id, '') AS brand_id,
              coalesce(card_brand_name, '') AS brand_name,
              count() AS transaction_count,
              sum(total_amount) AS total_amount,
              sum(if(vat_discount_applied = 1, discount_amount, 0)) AS total_vat_discounts,
              avg(installment_count) AS avg_installments
   FROM fact_sale_payments
   WHERE active = 1 AND card_brand_id IS NOT NULL
   GROUP BY company_id, store_id, the_date, brand_id, brand_name;