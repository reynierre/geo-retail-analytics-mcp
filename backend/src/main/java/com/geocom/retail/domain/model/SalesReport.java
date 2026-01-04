package com.geocom.retail.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Value Object representing a sales report for a store.
 * Immutable by design using Java Record.
 */
public record SalesReport(
    Long storeId,
    String storeCode,
    String storeName,
    long ticketCount,
    BigDecimal totalSales,
    BigDecimal averageTicket,
    BigDecimal totalDiscounts,
    LocalDate startDate,
    LocalDate endDate
) {
    public SalesReport {
        if (storeId == null || storeId <= 0) {
            throw new IllegalArgumentException("Store ID must be positive");
        }
        if (totalSales != null && totalSales.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total sales cannot be negative");
        }
    }
}
