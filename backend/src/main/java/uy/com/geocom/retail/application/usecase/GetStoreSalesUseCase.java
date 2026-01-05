package uy.com.geocom.retail.application.usecase;

import uy.com.geocom.retail.domain.exception.InvalidDateRangeException;
import uy.com.geocom.retail.domain.exception.StoreNotFoundException;
import uy.com.geocom.retail.domain.model.SalesReport;
import uy.com.geocom.retail.domain.repository.SalesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Use case for getting sales data for a specific store.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetStoreSalesUseCase {

    private final SalesRepository salesRepository;

    public SalesReport execute(Long companyId, String storeCode, LocalDate startDate, LocalDate endDate) {
        log.info("Getting sales for store {} (company {}) from {} to {}",
                 storeCode, companyId, startDate, endDate);

        validateDateRange(startDate, endDate);

        Long storeId = salesRepository.findStoreIdByCode(companyId, storeCode)
            .orElseThrow(() -> new StoreNotFoundException("Store not found: " + storeCode));

        return salesRepository.findSalesByStoreAndPeriod(companyId, storeId, startDate, endDate);
    }

    private void validateDateRange(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new InvalidDateRangeException("Start date must be before or equal to end date");
        }
        if (start.isBefore(LocalDate.now().minusYears(5))) {
            throw new InvalidDateRangeException("Cannot query data older than 5 years");
        }
    }
}
