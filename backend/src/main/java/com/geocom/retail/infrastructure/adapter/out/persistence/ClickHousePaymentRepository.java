package com.geocom.retail.infrastructure.adapter.out.persistence;

import com.geocom.retail.domain.model.CardBrandSummary;
import com.geocom.retail.domain.model.PaymentSummary;
import com.geocom.retail.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * ClickHouse implementation of PaymentRepository.
 * Uses mv_payment_summary and mv_card_brand_summary for optimized queries.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ClickHousePaymentRepository implements PaymentRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<PaymentSummary> findPaymentSummary(Long companyId, Long storeId,
                                                    LocalDate start, LocalDate end) {
        log.debug("Querying payment summary for company={}, store={}, period={} to {}",
                  companyId, storeId, start, end);

        String baseSql = """
            SELECT
                mv.payment_method_id,
                pm.payment_method_name,
                pm.payment_type,
                sum(mv.payment_count) AS payment_count,
                sum(mv.total_amount) AS total_amount,
                sum(mv.total_discounts) AS total_discounts
            FROM mv_payment_summary mv
            LEFT JOIN dim_payment_methods pm ON pm.payment_method_id = mv.payment_method_id
            WHERE mv.company_id = ?
                AND mv.the_date BETWEEN ? AND ?
            """;

        String sql;
        Object[] params;

        if (storeId != null) {
            sql = baseSql + """
                    AND mv.store_id = ?
                GROUP BY mv.payment_method_id, pm.payment_method_name, pm.payment_type
                ORDER BY total_amount DESC
                """;
            params = new Object[]{companyId, start, end, storeId};
        } else {
            sql = baseSql + """
                GROUP BY mv.payment_method_id, pm.payment_method_name, pm.payment_type
                ORDER BY total_amount DESC
                """;
            params = new Object[]{companyId, start, end};
        }

        return jdbcTemplate.query(sql, (rs, rowNum) -> new PaymentSummary(
            rs.getString("payment_method_id"),
            rs.getString("payment_method_name"),
            rs.getString("payment_type"),
            rs.getLong("payment_count"),
            rs.getBigDecimal("total_amount"),
            rs.getBigDecimal("total_discounts"),
            null // avgInstallments not available in mv_payment_summary
        ), params);
    }

    @Override
    public List<CardBrandSummary> findCardBrandSummary(Long companyId, Long storeId,
                                                        LocalDate start, LocalDate end) {
        log.debug("Querying card brand summary for company={}, store={}, period={} to {}",
                  companyId, storeId, start, end);

        String baseSql = """
            SELECT
                brand_id AS card_brand_id,
                brand_name AS card_brand_name,
                sum(transaction_count) AS transaction_count,
                sum(total_amount) AS total_amount,
                sum(total_vat_discounts) AS vat_discounts,
                avg(avg_installments) AS avg_installments
            FROM mv_card_brand_summary
            WHERE company_id = ?
                AND the_date BETWEEN ? AND ?
            """;

        String sql;
        Object[] params;

        if (storeId != null) {
            sql = baseSql + """
                    AND store_id = ?
                GROUP BY card_brand_id, card_brand_name
                ORDER BY total_amount DESC
                """;
            params = new Object[]{companyId, start, end, storeId};
        } else {
            sql = baseSql + """
                GROUP BY card_brand_id, card_brand_name
                ORDER BY total_amount DESC
                """;
            params = new Object[]{companyId, start, end};
        }

        return jdbcTemplate.query(sql, (rs, rowNum) -> new CardBrandSummary(
            rs.getString("card_brand_id"),
            rs.getString("card_brand_name"),
            rs.getLong("transaction_count"),
            rs.getBigDecimal("total_amount"),
            rs.getBigDecimal("vat_discounts"),
            rs.getBigDecimal("avg_installments")
        ), params);
    }
}
