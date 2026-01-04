package com.geocom.retail.application.usecase;

import com.geocom.retail.domain.model.PaymentSummary;
import com.geocom.retail.domain.repository.PaymentRepository;
import com.geocom.retail.domain.repository.SalesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Use case for getting payment method summary.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetPaymentSummaryUseCase {

    private final PaymentRepository paymentRepository;
    private final SalesRepository salesRepository;

    public List<PaymentSummary> execute(Long companyId, String storeCode,
                                        LocalDate startDate, LocalDate endDate) {
        log.info("Getting payment summary for company {}, store={}, period={}-{}",
                 companyId, storeCode, startDate, endDate);

        Long storeId = null;
        if (storeCode != null && !storeCode.isBlank()) {
            storeId = salesRepository.findStoreIdByCode(companyId, storeCode).orElse(null);
        }

        return paymentRepository.findPaymentSummary(companyId, storeId, startDate, endDate);
    }
}
