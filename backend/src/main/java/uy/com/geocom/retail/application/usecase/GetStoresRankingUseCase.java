package uy.com.geocom.retail.application.usecase;

import uy.com.geocom.retail.domain.exception.InvalidDateRangeException;
import uy.com.geocom.retail.domain.model.StoreRanking;
import uy.com.geocom.retail.domain.repository.SalesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Use case for getting stores ranking by sales.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetStoresRankingUseCase {

    private static final int MAX_LIMIT = 100;
    private final SalesRepository salesRepository;

    public List<StoreRanking> execute(Long companyId, LocalDate startDate, LocalDate endDate, int limit) {
        log.info("Getting stores ranking for company {} from {} to {}, limit {}",
                 companyId, startDate, endDate, limit);

        validateDateRange(startDate, endDate);
        int effectiveLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);

        return salesRepository.findStoresRanking(companyId, startDate, endDate, effectiveLimit);
    }

    private void validateDateRange(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new InvalidDateRangeException("Start date must be before or equal to end date");
        }
    }
}
