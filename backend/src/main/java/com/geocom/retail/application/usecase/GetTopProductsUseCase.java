package com.geocom.retail.application.usecase;

import com.geocom.retail.domain.model.ProductSales;
import com.geocom.retail.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Use case for getting top selling products.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetTopProductsUseCase {

    private static final int MAX_LIMIT = 50;
    private final ProductRepository productRepository;

    public List<ProductSales> execute(Long companyId, LocalDate startDate, LocalDate endDate,
                                      String category, int limit) {
        log.info("Getting top products for company {}, category={}, period={}-{}",
                 companyId, category, startDate, endDate);

        int effectiveLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        return productRepository.findTopProducts(companyId, startDate, endDate, category, effectiveLimit);
    }
}
