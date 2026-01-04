package com.geocom.retail.domain.model;

import java.math.BigDecimal;

/**
 * Value Object representing a store's position in a sales ranking.
 */
public record StoreRanking(
    int rank,
    Long storeId,
    String storeCode,
    String storeName,
    String region,
    long ticketCount,
    BigDecimal totalSales,
    BigDecimal averageTicket
) {}
