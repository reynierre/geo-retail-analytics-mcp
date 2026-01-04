package com.geocom.retail.domain.model;

import java.math.BigDecimal;

/**
 * Value Object representing sales data for a product.
 */
public record ProductSales(
    Long productId,
    String productName,
    String category,
    String subcategory,
    BigDecimal quantitySold,
    BigDecimal totalRevenue,
    BigDecimal totalDiscounts,
    long timesSold
) {}
