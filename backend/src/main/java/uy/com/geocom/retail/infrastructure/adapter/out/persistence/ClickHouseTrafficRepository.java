package uy.com.geocom.retail.infrastructure.adapter.out.persistence;

import uy.com.geocom.retail.domain.model.HourlyTraffic;
import uy.com.geocom.retail.domain.repository.TrafficRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * ClickHouse implementation of TrafficRepository.
 * Uses mv_hourly_sales for optimized traffic queries.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ClickHouseTrafficRepository implements TrafficRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<HourlyTraffic> findHourlyTraffic(Long companyId, Long storeId, LocalDate date) {
        log.debug("Querying hourly traffic for company={}, store={}, date={}", companyId, storeId, date);

        String sql;
        Object[] params;

        if (storeId != null) {
            sql = """
                SELECT
                    the_date,
                    the_hour,
                    sum(ticket_count) AS ticket_count,
                    sum(total_sales) AS total_sales
                FROM mv_hourly_sales
                WHERE company_id = ?
                    AND store_id = ?
                    AND the_date = ?
                GROUP BY the_date, the_hour
                ORDER BY the_hour
                """;
            params = new Object[]{companyId, storeId, date};
        } else {
            // Aggregate across all stores
            sql = """
                SELECT
                    the_date,
                    the_hour,
                    sum(ticket_count) AS ticket_count,
                    sum(total_sales) AS total_sales
                FROM mv_hourly_sales
                WHERE company_id = ?
                    AND the_date = ?
                GROUP BY the_date, the_hour
                ORDER BY the_hour
                """;
            params = new Object[]{companyId, date};
        }

        return jdbcTemplate.query(sql, (rs, rowNum) -> new HourlyTraffic(
            rs.getDate("the_date").toLocalDate(),
            rs.getInt("the_hour"),
            rs.getLong("ticket_count"),
            rs.getBigDecimal("total_sales")
        ), params);
    }
}
