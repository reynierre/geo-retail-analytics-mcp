package uy.com.geocom.retail.application.usecase;

import uy.com.geocom.retail.domain.exception.StoreNotFoundException;
import uy.com.geocom.retail.domain.model.HourlyTraffic;
import uy.com.geocom.retail.domain.repository.SalesRepository;
import uy.com.geocom.retail.domain.repository.TrafficRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Use case for getting hourly traffic data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetHourlyTrafficUseCase {

    private final TrafficRepository trafficRepository;
    private final SalesRepository salesRepository;

    public List<HourlyTraffic> execute(Long companyId, String storeCode, LocalDate date) {
        log.info("Getting hourly traffic for store {} on {}", storeCode, date);

        Long storeId = null;
        if (storeCode != null && !storeCode.isBlank()) {
            storeId = salesRepository.findStoreIdByCode(companyId, storeCode)
                .orElseThrow(() -> new StoreNotFoundException("Store not found: " + storeCode));
        }

        return trafficRepository.findHourlyTraffic(companyId, storeId, date);
    }
}
