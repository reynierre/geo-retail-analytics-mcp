package uy.com.geocom.retail.domain.model;

import java.math.BigDecimal;

/**
 * Value Object representing a card brand summary.
 */
public record CardBrandSummary(
    String cardBrandId,
    String cardBrandName,
    long transactionCount,
    BigDecimal totalAmount,
    BigDecimal vatDiscounts,
    BigDecimal avgInstallments
) {}
