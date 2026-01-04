package com.geocom.retail.domain.repository;

import com.geocom.retail.domain.model.CardBrandSummary;
import com.geocom.retail.domain.model.PaymentSummary;

import java.time.LocalDate;
import java.util.List;

/**
 * Port for payment data access.
 */
public interface PaymentRepository {

    /**
     * Find payment summary by payment method.
     */
    List<PaymentSummary> findPaymentSummary(Long companyId, Long storeId,
                                            LocalDate start, LocalDate end);

    /**
     * Find card brand summary.
     */
    List<CardBrandSummary> findCardBrandSummary(Long companyId, Long storeId,
                                                LocalDate start, LocalDate end);
}
