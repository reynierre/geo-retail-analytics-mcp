package com.geocom.retail.domain.repository;

import com.geocom.retail.domain.model.SalesReport;
import com.geocom.retail.domain.model.StoreRanking;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Port for sales data access.
 * Defined in domain layer, implemented in infrastructure.
 */
public interface SalesRepository {

    /**
     * Find sales report for a specific store in a date range.
     */
    SalesReport findSalesByStoreAndPeriod(Long companyId, Long storeId, LocalDate start, LocalDate end);

    /**
     * Find stores ranking ordered by total sales.
     */
    List<StoreRanking> findStoresRanking(Long companyId, LocalDate start, LocalDate end, int limit);

    /**
     * Check if a store exists.
     */
    boolean existsStore(Long companyId, Long storeId);

    /**
     * Find store ID by its code.
     */
    Optional<Long> findStoreIdByCode(Long companyId, String storeCode);
}
