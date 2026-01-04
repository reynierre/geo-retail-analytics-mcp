-- =====================================================
-- Geo Retail Analytics - Seed Data
-- Run after 001_create_tables.sql
-- =====================================================

USE geo_retail_analytics;

-- =====================================================
-- SEED: Companies
-- =====================================================
INSERT INTO dim_companies (company_id, company_name, tax_number, country_code) VALUES
                                                                                   (1, 'Geocom Uruguay S.A.', '214567890012', 'UY'),
                                                                                   (2, 'Retail Demo S.A.', '219876543210', 'UY'),
                                                                                   (3, 'Supermercados del Este', '215432109876', 'UY');

-- =====================================================
-- SEED: Stores
-- =====================================================
INSERT INTO dim_stores (store_id, company_id, store_code, store_name, region, city, store_format) VALUES
                                                                                                      (1, 1, '001', 'Sucursal Pocitos', 'Montevideo', 'Montevideo', 'super'),
                                                                                                      (2, 1, '002', 'Sucursal Carrasco', 'Montevideo', 'Montevideo', 'hiper'),
                                                                                                      (3, 1, '003', 'Sucursal Punta del Este', 'Este', 'Maldonado', 'super'),
                                                                                                      (4, 1, '004', 'Sucursal Colonia', 'Litoral', 'Colonia', 'express'),
                                                                                                      (5, 1, '005', 'Sucursal Salto', 'Norte', 'Salto', 'super'),
                                                                                                      (6, 2, '001', 'Demo Centro', 'Montevideo', 'Montevideo', 'super'),
                                                                                                      (7, 2, '002', 'Demo Shopping', 'Montevideo', 'Montevideo', 'express'),
                                                                                                      (8, 2, '003', 'Demo Costa', 'Este', 'Punta del Este', 'hiper'),
                                                                                                      (9, 3, '001', 'Este Maldonado', 'Este', 'Maldonado', 'hiper'),
                                                                                                      (10, 3, '002', 'Este Rocha', 'Este', 'Rocha', 'super');

-- =====================================================
-- SEED: Payment Methods
-- =====================================================
INSERT INTO dim_payment_methods (payment_method_id, payment_method_name, payment_type) VALUES
                                                                                           ('EF', 'Efectivo', 'CASH'),
                                                                                           ('TC', 'Tarjeta Crédito', 'CARD'),
                                                                                           ('TD', 'Tarjeta Débito', 'CARD'),
                                                                                           ('CH', 'Cheque', 'CHECK'),
                                                                                           ('TR', 'Transferencia', 'TRANSFER'),
                                                                                           ('CTA', 'Cuenta Corriente', 'ACCOUNT'),
                                                                                           ('QR', 'QR/Billetera Digital', 'DIGITAL');

-- =====================================================
-- SEED: Taxes (Uruguay)
-- =====================================================
INSERT INTO dim_taxes (tax_code, tax_name, tax_rate, tax_type, country_code) VALUES
                                                                                 ('22', 'IVA Básico', 22.00, 'IVA', 'UY'),
                                                                                 ('10', 'IVA Mínimo', 10.00, 'IVA', 'UY'),
                                                                                 ('0', 'Exento', 0.00, 'EXENTO', 'UY');

-- =====================================================
-- SEED: Card Brands
-- =====================================================
INSERT INTO dim_card_brands (card_brand_id, card_brand_name, card_network) VALUES
                                                                               ('VISA', 'Visa', 'VISA'),
                                                                               ('MC', 'Mastercard', 'MASTERCARD'),
                                                                               ('AMEX', 'American Express', 'AMEX'),
                                                                               ('OCA', 'OCA', 'LOCAL'),
                                                                               ('CABAL', 'Cabal', 'LOCAL'),
                                                                               ('ANDA', 'Anda', 'LOCAL'),
                                                                               ('PASS', 'Passcard', 'LOCAL'),
                                                                               ('CRED', 'Creditel', 'LOCAL');

-- =====================================================
-- SEED: Products (100 sample products)
-- =====================================================
INSERT INTO dim_products (product_id, company_id, barcode, product_name, category, subcategory, brand, unit_of_measure) VALUES
                                                                                                                            (1, 1, '7891234560001', 'Leche Entera 1L', 'Lácteos', 'Leches', 'Conaprole', 'UN'),
                                                                                                                            (2, 1, '7891234560002', 'Leche Descremada 1L', 'Lácteos', 'Leches', 'Conaprole', 'UN'),
                                                                                                                            (3, 1, '7891234560003', 'Yogur Natural 200g', 'Lácteos', 'Yogures', 'Conaprole', 'UN'),
                                                                                                                            (4, 1, '7891234560004', 'Queso Colonia 1kg', 'Lácteos', 'Quesos', 'Conaprole', 'KG'),
                                                                                                                            (5, 1, '7891234560005', 'Manteca 200g', 'Lácteos', 'Mantecas', 'Conaprole', 'UN'),
                                                                                                                            (6, 1, '7891234560006', 'Pan Blanco 500g', 'Panadería', 'Panes', 'Bimbo', 'UN'),
                                                                                                                            (7, 1, '7891234560007', 'Pan Integral 500g', 'Panadería', 'Panes', 'Bimbo', 'UN'),
                                                                                                                            (8, 1, '7891234560008', 'Medialunas x6', 'Panadería', 'Facturas', 'Artesanal', 'UN'),
                                                                                                                            (9, 1, '7891234560009', 'Arroz Blanco 1kg', 'Almacén', 'Arroces', 'Saman', 'KG'),
                                                                                                                            (10, 1, '7891234560010', 'Arroz Integral 1kg', 'Almacén', 'Arroces', 'Saman', 'KG'),
                                                                                                                            (11, 1, '7891234560011', 'Fideos Spaghetti 500g', 'Almacén', 'Pastas', 'Adria', 'UN'),
                                                                                                                            (12, 1, '7891234560012', 'Fideos Tirabuzón 500g', 'Almacén', 'Pastas', 'Adria', 'UN'),
                                                                                                                            (13, 1, '7891234560013', 'Aceite Girasol 1L', 'Almacén', 'Aceites', 'Óptimo', 'UN'),
                                                                                                                            (14, 1, '7891234560014', 'Aceite Oliva 500ml', 'Almacén', 'Aceites', 'Cocinero', 'UN'),
                                                                                                                            (15, 1, '7891234560015', 'Azúcar 1kg', 'Almacén', 'Azúcares', 'Bella Unión', 'KG'),
                                                                                                                            (16, 1, '7891234560016', 'Yerba Mate 1kg', 'Almacén', 'Infusiones', 'Canarias', 'KG'),
                                                                                                                            (17, 1, '7891234560017', 'Café Molido 250g', 'Almacén', 'Cafés', 'Aguila', 'UN'),
                                                                                                                            (18, 1, '7891234560018', 'Té Negro x25', 'Almacén', 'Infusiones', 'La Virginia', 'UN'),
                                                                                                                            (19, 1, '7891234560019', 'Galletitas Dulces 300g', 'Almacén', 'Galletitas', 'Portezuelo', 'UN'),
                                                                                                                            (20, 1, '7891234560020', 'Galletitas Saladas 300g', 'Almacén', 'Galletitas', 'Famosa', 'UN'),
                                                                                                                            (21, 1, '7891234560021', 'Agua Mineral 1.5L', 'Bebidas', 'Aguas', 'Salus', 'UN'),
                                                                                                                            (22, 1, '7891234560022', 'Agua Saborizada 1.5L', 'Bebidas', 'Aguas', 'Salus', 'UN'),
                                                                                                                            (23, 1, '7891234560023', 'Refresco Cola 2L', 'Bebidas', 'Refrescos', 'Coca-Cola', 'UN'),
                                                                                                                            (24, 1, '7891234560024', 'Refresco Naranja 2L', 'Bebidas', 'Refrescos', 'Fanta', 'UN'),
                                                                                                                            (25, 1, '7891234560025', 'Jugo Natural 1L', 'Bebidas', 'Jugos', 'Conaprole', 'UN'),
                                                                                                                            (26, 1, '7891234560026', 'Cerveza Lager 1L', 'Bebidas', 'Cervezas', 'Pilsen', 'UN'),
                                                                                                                            (27, 1, '7891234560027', 'Cerveza Negra 1L', 'Bebidas', 'Cervezas', 'Patricia', 'UN'),
                                                                                                                            (28, 1, '7891234560028', 'Vino Tinto Tannat 750ml', 'Bebidas', 'Vinos', 'Pisano', 'UN'),
                                                                                                                            (29, 1, '7891234560029', 'Vino Blanco 750ml', 'Bebidas', 'Vinos', 'Bouza', 'UN'),
                                                                                                                            (30, 1, '7891234560030', 'Jabón Tocador x3', 'Limpieza', 'Jabones', 'Rexona', 'UN'),
                                                                                                                            (31, 1, '7891234560031', 'Shampoo 400ml', 'Limpieza', 'Cabello', 'Sedal', 'UN'),
                                                                                                                            (32, 1, '7891234560032', 'Detergente 750ml', 'Limpieza', 'Lavado', 'Nevex', 'UN'),
                                                                                                                            (33, 1, '7891234560033', 'Suavizante 1L', 'Limpieza', 'Lavado', 'Comfort', 'UN'),
                                                                                                                            (34, 1, '7891234560034', 'Lavandina 1L', 'Limpieza', 'Desinfectantes', 'Agua Jane', 'UN'),
                                                                                                                            (35, 1, '7891234560035', 'Papel Higiénico x4', 'Limpieza', 'Papeles', 'Elite', 'UN'),
                                                                                                                            (36, 1, '7891234560036', 'Carne Picada 1kg', 'Carnes', 'Vacuna', 'Frigorífico', 'KG'),
                                                                                                                            (37, 1, '7891234560037', 'Asado 1kg', 'Carnes', 'Vacuna', 'Frigorífico', 'KG'),
                                                                                                                            (38, 1, '7891234560038', 'Pollo Entero 2kg', 'Carnes', 'Aves', 'Avícola', 'KG'),
                                                                                                                            (39, 1, '7891234560039', 'Chorizo Parrillero x4', 'Carnes', 'Embutidos', 'Schneck', 'UN'),
                                                                                                                            (40, 1, '7891234560040', 'Jamón Cocido 200g', 'Carnes', 'Fiambres', 'Ottonello', 'UN'),
                                                                                                                            (41, 1, '7891234560041', 'Manzana Roja 1kg', 'Frutas', 'Manzanas', 'Nacional', 'KG'),
                                                                                                                            (42, 1, '7891234560042', 'Banana 1kg', 'Frutas', 'Tropicales', 'Importada', 'KG'),
                                                                                                                            (43, 1, '7891234560043', 'Naranja 1kg', 'Frutas', 'Cítricos', 'Nacional', 'KG'),
                                                                                                                            (44, 1, '7891234560044', 'Tomate 1kg', 'Verduras', 'Frutos', 'Nacional', 'KG'),
                                                                                                                            (45, 1, '7891234560045', 'Lechuga', 'Verduras', 'Hojas', 'Nacional', 'UN'),
                                                                                                                            (46, 1, '7891234560046', 'Papa 1kg', 'Verduras', 'Tubérculos', 'Nacional', 'KG'),
                                                                                                                            (47, 1, '7891234560047', 'Cebolla 1kg', 'Verduras', 'Bulbos', 'Nacional', 'KG'),
                                                                                                                            (48, 1, '7891234560048', 'Zanahoria 1kg', 'Verduras', 'Raíces', 'Nacional', 'KG'),
                                                                                                                            (49, 1, '7891234560049', 'Helado Vainilla 1L', 'Congelados', 'Helados', 'Crufi', 'UN'),
                                                                                                                            (50, 1, '7891234560050', 'Pizza Congelada', 'Congelados', 'Comidas', 'Sibarita', 'UN');

-- =====================================================
-- SEED: Promotions
-- =====================================================
INSERT INTO dim_promotions (promotion_id, company_id, promotion_name, promotion_type, promotion_category, campaign_name, start_date, financed_discount) VALUES
                                                                                                                                                            ('PROMO-001', 1, '2x1 Lácteos', 'NXM', 'REGULAR', 'Verano 2025', '2025-01-01', 0),
                                                                                                                                                            ('PROMO-002', 1, '30% Bebidas', 'PORCENTAJE', 'FIN_DE_SEMANA', 'Verano 2025', '2025-01-01', 0),
                                                                                                                                                            ('PROMO-003', 1, '20% Carnes', 'PORCENTAJE', 'REGULAR', 'Verano 2025', '2025-01-01', 0),
                                                                                                                                                            ('PROMO-004', 1, '3x2 Limpieza', 'NXM', 'REGULAR', NULL, '2025-01-01', 0),
                                                                                                                                                            ('PROMO-005', 1, '15% VISA', 'PORCENTAJE', 'BANCO', 'Convenio VISA', '2025-01-01', 1),
                                                                                                                                                            ('PROMO-006', 1, '10% OCA', 'PORCENTAJE', 'BANCO', 'Convenio OCA', '2025-01-01', 1),
                                                                                                                                                            ('PROMO-007', 2, '25% Todo', 'PORCENTAJE', 'ANIVERSARIO', 'Aniversario Demo', '2025-01-01', 0),
                                                                                                                                                            ('PROMO-008', 2, '50% Segunda Unidad', 'PORCENTAJE', 'REGULAR', NULL, '2025-01-01', 0),
                                                                                                                                                            ('PROMO-009', 3, '20% Frescos', 'PORCENTAJE', 'REGULAR', NULL, '2025-01-01', 0),
                                                                                                                                                            ('PROMO-010', 3, 'Combo Asado', 'COMBO', 'FIN_DE_SEMANA', 'Fin de Semana', '2025-01-01', 0);

-- =====================================================
-- SEED: Sample Sales (1000 transactions)
-- =====================================================
INSERT INTO fact_sales (
    company_id, store_id, pos_terminal_id, ticket_number, sale_date,
    active, created_by, updated_by, cashier_id, currency_code,
    subtotal_amount, discount_amount, tax_amount, rounding_amount, surcharge_amount, total_amount,
    fiscal_document_type_id, fiscal_document_series, fiscal_document_number,
    customer_tax_type_id, customer_tax_number
)
SELECT
    1 AS company_id,
    (rand() % 5) + 1 AS store_id,
    lpad(toString((rand() % 4) + 1), 2, '0') AS pos_terminal_id,
    toString(number + 10000) AS ticket_number,
    now() - toIntervalDay(rand() % 30) - toIntervalHour(8 + (rand() % 12)) - toIntervalMinute(rand() % 60) AS sale_date,
    1 AS active,
    100 + (rand() % 10) AS created_by,
    100 + (rand() % 10) AS updated_by,
    lpad(toString((rand() % 8) + 1), 3, '0') AS cashier_id,
    'UYU' AS currency_code,
    toDecimal64(1000 + (rand() % 9000), 2) AS subtotal_amount,
    toDecimal64(rand() % 500, 2) AS discount_amount,
    toDecimal64(200 + (rand() % 1800), 2) AS tax_amount,
    toDecimal64((rand() % 100) - 50, 2) / 100 AS rounding_amount,
    0 AS surcharge_amount,
    toDecimal64(1200 + (rand() % 10800), 2) AS total_amount,
    arrayElement(['101', '111'], (rand() % 2) + 1) AS fiscal_document_type_id,
    'A' AS fiscal_document_series,
    toString(1000000 + number) AS fiscal_document_number,
    arrayElement([0, 1000000, 1000001], (rand() % 3) + 1) AS customer_tax_type_id,
    if(rand() % 3 = 0, concat('21', lpad(toString(rand() % 100000000), 10, '0')), NULL) AS customer_tax_number
FROM numbers(1000);

-- =====================================================
-- SEED: Sample Sale Items (3-8 items per sale)
-- =====================================================
INSERT INTO fact_sale_items (
    company_id, store_id, pos_terminal_id, ticket_number, sale_date,
    line_number, active, product_id, barcode,
    quantity, gross_unit_price, discount_amount, net_unit_price,
    subtotal_amount, tax_amount, tax_code, total_amount
)
SELECT
    s.company_id,
    s.store_id,
    s.pos_terminal_id,
    s.ticket_number,
    s.sale_date,
    row_number() OVER (PARTITION BY s.ticket_number ORDER BY rand()) AS line_number,
    1 AS active,
    (rand() % 50) + 1 AS product_id,
    concat('789123456', lpad(toString((rand() % 50) + 1), 4, '0')) AS barcode,
    toDecimal64((rand() % 5) + 1, 3) AS quantity,
    toDecimal64(50 + (rand() % 500), 2) AS gross_unit_price,
    toDecimal64(rand() % 50, 2) AS discount_amount,
    toDecimal64(50 + (rand() % 450), 2) AS net_unit_price,
    toDecimal64(100 + (rand() % 2000), 2) AS subtotal_amount,
    toDecimal64(20 + (rand() % 400), 2) AS tax_amount,
    arrayElement(['22', '10', '0'], (rand() % 3) + 1) AS tax_code,
    toDecimal64(120 + (rand() % 2400), 2) AS total_amount
FROM fact_sales s
    ARRAY JOIN range(1, (rand() % 6) + 3) AS item_num;

-- =====================================================
-- SEED: Sample Sale Payments (1-2 payments per sale)
-- =====================================================
INSERT INTO fact_sale_payments (
    company_id, store_id, pos_terminal_id, ticket_number, sale_date,
    payment_sequence, active, payment_method_id, currency_code,
    tendered_amount, tendered_amount_local, total_amount,
    discount_amount, surcharge_amount,
    change_payment, online_transaction,
    card_brand_id, card_brand_name, card_type,
    installment_count, vat_discount_applied
)
SELECT
    company_id,
    store_id,
    pos_terminal_id,
    ticket_number,
    sale_date,
    1 AS payment_sequence,
    1 AS active,
    arrayElement(['EF', 'TC', 'TD', 'QR'], (rand() % 4) + 1) AS payment_method_id,
    'UYU' AS currency_code,
    total_amount AS tendered_amount,
    total_amount AS tendered_amount_local,
    total_amount,
    toDecimal64(rand() % 100, 2) AS discount_amount,
    0 AS surcharge_amount,
    0 AS change_payment,
    1 AS online_transaction,
    if(payment_method_id IN ('TC', 'TD'), arrayElement(['VISA', 'MC', 'OCA', 'CABAL'], (rand() % 4) + 1), NULL) AS card_brand_id,
    if(payment_method_id IN ('TC', 'TD'), arrayElement(['Visa', 'Mastercard', 'OCA', 'Cabal'], (rand() % 4) + 1), NULL) AS card_brand_name,
    if(payment_method_id = 'TC', 'C', if(payment_method_id = 'TD', 'D', NULL)) AS card_type,
    if(payment_method_id = 'TC', arrayElement([1, 3, 6, 12], (rand() % 4) + 1), 1) AS installment_count,
    if(payment_method_id IN ('TC', 'TD') AND rand() % 3 = 0, 1, 0) AS vat_discount_applied
FROM fact_sales;

-- =====================================================
-- Verify Data
-- =====================================================
SELECT 'dim_companies' AS table_name, count() AS row_count FROM dim_companies
UNION ALL SELECT 'dim_stores', count() FROM dim_stores
UNION ALL SELECT 'dim_products', count() FROM dim_products
UNION ALL SELECT 'dim_payment_methods', count() FROM dim_payment_methods
UNION ALL SELECT 'dim_taxes', count() FROM dim_taxes
UNION ALL SELECT 'dim_card_brands', count() FROM dim_card_brands
UNION ALL SELECT 'dim_promotions', count() FROM dim_promotions
UNION ALL SELECT 'fact_sales', count() FROM fact_sales
UNION ALL SELECT 'fact_sale_items', count() FROM fact_sale_items
UNION ALL SELECT 'fact_sale_payments', count() FROM fact_sale_payments;