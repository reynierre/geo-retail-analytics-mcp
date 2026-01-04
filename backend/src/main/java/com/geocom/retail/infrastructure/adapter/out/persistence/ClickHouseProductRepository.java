package com.geocom.retail.infrastructure.adapter.out.persistence;

import com.geocom.retail.domain.model.ProductSales;
import com.geocom.retail.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * ClickHouse implementation of ProductRepository.
 * Uses materialized views for optimized product sales queries.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ClickHouseProductRepository implements ProductRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<ProductSales> findTopProducts(Long companyId, LocalDate start, LocalDate end,
                                               String category, int limit) {
        log.debug("Querying top products for company={}, period={} to {}, category={}, limit={}",
                  companyId, start, end, category, limit);

        String baseSql = """
            SELECT
                dp.product_id,
                dp.product_name,
                dp.category,
                dp.subcategory,
                sum(mv.quantity_sold) AS quantity_sold,
                sum(mv.total_revenue) AS total_revenue,
                sum(mv.total_discounts) AS total_discounts,
                sum(mv.times_sold) AS times_sold
            FROM dim_products dp
            INNER JOIN mv_product_sales mv ON mv.company_id = dp.company_id
                AND mv.product_id = dp.product_id
                AND mv.the_date BETWEEN ? AND ?
            WHERE dp.company_id = ?
                AND dp.active = 1
            """;

        String sql;
        Object[] params;

        if (category != null && !category.isBlank()) {
            sql = baseSql + """
                    AND dp.category = ?
                GROUP BY dp.product_id, dp.product_name, dp.category, dp.subcategory
                ORDER BY total_revenue DESC
                LIMIT ?
                """;
            params = new Object[]{start, end, companyId, category, limit};
        } else {
            sql = baseSql + """
                GROUP BY dp.product_id, dp.product_name, dp.category, dp.subcategory
                ORDER BY total_revenue DESC
                LIMIT ?
                """;
            params = new Object[]{start, end, companyId, limit};
        }

        return jdbcTemplate.query(sql, (rs, rowNum) -> new ProductSales(
            rs.getLong("product_id"),
            rs.getString("product_name"),
            rs.getString("category"),
            rs.getString("subcategory"),
            rs.getBigDecimal("quantity_sold"),
            rs.getBigDecimal("total_revenue"),
            rs.getBigDecimal("total_discounts"),
            rs.getLong("times_sold")
        ), params);
    }
}
