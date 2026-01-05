package uy.com.geocom.retail.infrastructure.adapter.out.persistence;

import uy.com.geocom.retail.domain.model.SalesReport;
import uy.com.geocom.retail.domain.model.StoreRanking;
import uy.com.geocom.retail.domain.repository.SalesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ClickHouse implementation of SalesRepository.
 * Uses materialized views for optimized queries.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ClickHouseSalesRepository implements SalesRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public SalesReport findSalesByStoreAndPeriod(Long companyId, Long storeId, LocalDate start, LocalDate end) {
        log.debug("Querying sales for company={}, store={}, period={} to {}", companyId, storeId, start, end);

        String sql = """
            SELECT
                ds.store_id,
                ds.store_code,
                ds.store_name,
                coalesce(sum(mv.ticket_count), 0) AS ticket_count,
                coalesce(sum(mv.total_sales), 0) AS total_sales,
                coalesce(sum(mv.total_discounts), 0) AS total_discounts
            FROM dim_stores ds
            LEFT JOIN mv_daily_sales mv ON mv.company_id = ds.company_id
                AND mv.store_id = ds.store_id
                AND mv.the_date BETWEEN ? AND ?
            WHERE ds.company_id = ?
                AND ds.store_id = ?
                AND ds.active = 1
            GROUP BY ds.store_id, ds.store_code, ds.store_name
            """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            long tickets = rs.getLong("ticket_count");
            BigDecimal sales = rs.getBigDecimal("total_sales");
            BigDecimal avgTicket = tickets > 0
                ? sales.divide(BigDecimal.valueOf(tickets), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

            return new SalesReport(
                rs.getLong("store_id"),
                rs.getString("store_code"),
                rs.getString("store_name"),
                tickets,
                sales,
                avgTicket,
                rs.getBigDecimal("total_discounts"),
                start,
                end
            );
        }, start, end, companyId, storeId);
    }

    @Override
    public List<StoreRanking> findStoresRanking(Long companyId, LocalDate start, LocalDate end, int limit) {
        log.debug("Querying stores ranking for company={}, period={} to {}, limit={}",
                  companyId, start, end, limit);

        String sql = """
            SELECT
                ds.store_id,
                ds.store_code,
                ds.store_name,
                ds.region,
                coalesce(sum(mv.ticket_count), 0) AS ticket_count,
                coalesce(sum(mv.total_sales), 0) AS total_sales
            FROM dim_stores ds
            LEFT JOIN mv_daily_sales mv ON mv.company_id = ds.company_id
                AND mv.store_id = ds.store_id
                AND mv.the_date BETWEEN ? AND ?
            WHERE ds.company_id = ?
                AND ds.active = 1
            GROUP BY ds.store_id, ds.store_code, ds.store_name, ds.region
            ORDER BY total_sales DESC
            LIMIT ?
            """;

        List<StoreRanking> results = jdbcTemplate.query(sql, (rs, rowNum) -> {
            long tickets = rs.getLong("ticket_count");
            BigDecimal sales = rs.getBigDecimal("total_sales");
            BigDecimal avgTicket = tickets > 0
                ? sales.divide(BigDecimal.valueOf(tickets), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

            return new StoreRanking(
                0, // rank will be set below
                rs.getLong("store_id"),
                rs.getString("store_code"),
                rs.getString("store_name"),
                rs.getString("region"),
                tickets,
                sales,
                avgTicket
            );
        }, start, end, companyId, limit);

        // Assign ranks
        return java.util.stream.IntStream.range(0, results.size())
            .mapToObj(i -> {
                StoreRanking sr = results.get(i);
                return new StoreRanking(
                    i + 1,
                    sr.storeId(),
                    sr.storeCode(),
                    sr.storeName(),
                    sr.region(),
                    sr.ticketCount(),
                    sr.totalSales(),
                    sr.averageTicket()
                );
            })
            .toList();
    }

    @Override
    public boolean existsStore(Long companyId, Long storeId) {
        String sql = """
            SELECT count() > 0
            FROM dim_stores
            WHERE company_id = ? AND store_id = ? AND active = 1
            """;

        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, companyId, storeId);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public Optional<Long> findStoreIdByCode(Long companyId, String storeCode) {
        String sql = """
            SELECT store_id
            FROM dim_stores
            WHERE company_id = ? AND store_code = ? AND active = 1
            LIMIT 1
            """;

        List<Long> results = jdbcTemplate.queryForList(sql, Long.class, companyId, storeCode);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}
