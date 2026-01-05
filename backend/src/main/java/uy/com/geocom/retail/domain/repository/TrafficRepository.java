package uy.com.geocom.retail.domain.repository;

import uy.com.geocom.retail.domain.model.HourlyTraffic;

import java.time.LocalDate;
import java.util.List;

/**
 * Port for traffic data access.
 */
public interface TrafficRepository {

    /**
     * Find hourly traffic for a specific date and optionally a store.
     */
    List<HourlyTraffic> findHourlyTraffic(Long companyId, Long storeId, LocalDate date);
}
