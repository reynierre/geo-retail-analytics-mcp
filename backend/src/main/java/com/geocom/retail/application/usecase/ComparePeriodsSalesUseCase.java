package com.geocom.retail.application.usecase;

import com.geocom.retail.domain.exception.InvalidDateRangeException;
import com.geocom.retail.domain.model.PeriodComparison;
import com.geocom.retail.domain.model.StoreRanking;
import com.geocom.retail.domain.repository.SalesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Use case for comparing sales between two periods.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComparePeriodsSalesUseCase {

    private final SalesRepository salesRepository;

    public PeriodComparison execute(Long companyId,
                                    LocalDate period1Start, LocalDate period1End,
                                    LocalDate period2Start, LocalDate period2End) {
        log.info("Comparing periods {}-{} vs {}-{}",
                 period1Start, period1End, period2Start, period2End);

        validatePeriods(period1Start, period1End, period2Start, period2End);

        // Get aggregated sales for all stores in each period
        List<StoreRanking> ranking1 = salesRepository.findStoresRanking(companyId, period1Start, period1End, 1000);
        List<StoreRanking> ranking2 = salesRepository.findStoresRanking(companyId, period2Start, period2End, 1000);

        BigDecimal period1Sales = ranking1.stream()
            .map(StoreRanking::totalSales)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal period2Sales = ranking2.stream()
            .map(StoreRanking::totalSales)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long period1Tickets = ranking1.stream().mapToLong(StoreRanking::ticketCount).sum();
        long period2Tickets = ranking2.stream().mapToLong(StoreRanking::ticketCount).sum();

        BigDecimal salesVariation = calculateVariation(period1Sales, period2Sales);
        BigDecimal ticketVariation = calculateVariation(
            BigDecimal.valueOf(period1Tickets),
            BigDecimal.valueOf(period2Tickets)
        );

        return new PeriodComparison(
            period1Start, period1End,
            period2Start, period2End,
            period1Sales, period2Sales,
            period1Tickets, period2Tickets,
            salesVariation, ticketVariation
        );
    }

    private void validatePeriods(LocalDate p1Start, LocalDate p1End,
                                 LocalDate p2Start, LocalDate p2End) {
        if (p1Start.isAfter(p1End) || p2Start.isAfter(p2End)) {
            throw new InvalidDateRangeException("Start date must be before end date in each period");
        }
    }

    private BigDecimal calculateVariation(BigDecimal previous, BigDecimal current) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : new BigDecimal("100");
        }
        return current.subtract(previous)
            .divide(previous, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    }
}
