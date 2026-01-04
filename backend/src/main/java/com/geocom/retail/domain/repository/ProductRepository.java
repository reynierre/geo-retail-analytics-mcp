package com.geocom.retail.domain.repository;

import com.geocom.retail.domain.model.ProductSales;

import java.time.LocalDate;
import java.util.List;

/**
 * Port for product data access.
 */
public interface ProductRepository {

    /**
     * Find top selling products in a date range.
     */
    List<ProductSales> findTopProducts(Long companyId, LocalDate start, LocalDate end,
                                       String category, int limit);
}
