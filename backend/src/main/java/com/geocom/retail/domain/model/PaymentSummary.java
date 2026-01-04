package com.geocom.retail.domain.model;

import java.math.BigDecimal;

/**
 * Value Object representing a payment method summary.
 */
public record PaymentSummary(
    String paymentMethodId,
    String paymentMethodName,
    String paymentType,
    long paymentCount,
    BigDecimal totalAmount,
    BigDecimal totalDiscounts,
    BigDecimal avgInstallments
) {}
