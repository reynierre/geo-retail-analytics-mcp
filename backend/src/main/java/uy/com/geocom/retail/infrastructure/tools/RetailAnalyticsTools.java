package uy.com.geocom.retail.infrastructure.tools;

import uy.com.geocom.retail.application.usecase.*;
import uy.com.geocom.retail.domain.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Retail Analytics Tools for LLM integration.
 * Each method is annotated with @Tool and can be invoked by the LLM.
 * All tools delegate to domain use cases.
 */
@Component("retailAnalyticsTools")
@RequiredArgsConstructor
@Slf4j
public class RetailAnalyticsTools {

    private final GetStoreSalesUseCase getStoreSalesUseCase;
    private final GetStoresRankingUseCase getStoresRankingUseCase;
    private final GetTopProductsUseCase getTopProductsUseCase;
    private final GetHourlyTrafficUseCase getHourlyTrafficUseCase;
    private final ComparePeriodsSalesUseCase comparePeriodsSalesUseCase;
    private final GetPaymentSummaryUseCase getPaymentSummaryUseCase;
    private final GetCardBrandSummaryUseCase getCardBrandSummaryUseCase;

    @Value("${retail.default-company-id:1}")
    private Long defaultCompanyId;

    @Tool(name = "get_store_sales",
          description = "Obtiene las ventas totales de un local en un periodo. " +
                        "Devuelve total de ventas, cantidad de tickets y ticket promedio.")
    public SalesReport getStoreSales(
            @ToolParam(description = "Codigo del local, ej: '001', '045'") String storeCode,
            @ToolParam(description = "Fecha inicio en formato YYYY-MM-DD") String startDate,
            @ToolParam(description = "Fecha fin en formato YYYY-MM-DD") String endDate) {

        log.info("Tool get_store_sales called: store={}, period={} to {}", storeCode, startDate, endDate);

        return getStoreSalesUseCase.execute(
            defaultCompanyId,
            storeCode,
            LocalDate.parse(startDate),
            LocalDate.parse(endDate)
        );
    }

    @Tool(name = "get_stores_ranking",
          description = "Obtiene el ranking de locales ordenados por ventas totales. " +
                        "Incluye nombre, region, tickets y ventas de cada local.")
    public List<StoreRanking> getStoresRanking(
            @ToolParam(description = "Fecha inicio en formato YYYY-MM-DD") String startDate,
            @ToolParam(description = "Fecha fin en formato YYYY-MM-DD") String endDate,
            @ToolParam(description = "Cantidad maxima de locales a devolver (1-100)") int limit) {

        log.info("Tool get_stores_ranking called: period={} to {}, limit={}", startDate, endDate, limit);

        return getStoresRankingUseCase.execute(
            defaultCompanyId,
            LocalDate.parse(startDate),
            LocalDate.parse(endDate),
            limit
        );
    }

    @Tool(name = "get_top_products",
          description = "Obtiene los productos mas vendidos en un periodo. " +
                        "Puede filtrarse por categoria. Incluye cantidad vendida y revenue.")
    public List<ProductSales> getTopProducts(
            @ToolParam(description = "Fecha inicio en formato YYYY-MM-DD") String startDate,
            @ToolParam(description = "Fecha fin en formato YYYY-MM-DD") String endDate,
            @ToolParam(description = "Categoria de productos (opcional, dejar vacio para todas)") String category,
            @ToolParam(description = "Cantidad maxima de productos a devolver (1-50)") int limit) {

        log.info("Tool get_top_products called: period={} to {}, category={}, limit={}",
                 startDate, endDate, category, limit);

        return getTopProductsUseCase.execute(
            defaultCompanyId,
            LocalDate.parse(startDate),
            LocalDate.parse(endDate),
            category,
            limit
        );
    }

    @Tool(name = "get_hourly_traffic",
          description = "Obtiene el trafico de tickets por hora del dia. " +
                        "Util para analizar horarios pico. Puede filtrarse por local.")
    public List<HourlyTraffic> getHourlyTraffic(
            @ToolParam(description = "Codigo del local (opcional, vacio para todos)") String storeCode,
            @ToolParam(description = "Fecha a consultar en formato YYYY-MM-DD") String date) {

        log.info("Tool get_hourly_traffic called: store={}, date={}", storeCode, date);

        return getHourlyTrafficUseCase.execute(
            defaultCompanyId,
            storeCode,
            LocalDate.parse(date)
        );
    }

    @Tool(name = "compare_periods",
          description = "Compara ventas entre dos periodos. " +
                        "Calcula variacion porcentual en ventas y tickets.")
    public PeriodComparison comparePeriods(
            @ToolParam(description = "Fecha inicio periodo 1 (YYYY-MM-DD)") String period1Start,
            @ToolParam(description = "Fecha fin periodo 1 (YYYY-MM-DD)") String period1End,
            @ToolParam(description = "Fecha inicio periodo 2 (YYYY-MM-DD)") String period2Start,
            @ToolParam(description = "Fecha fin periodo 2 (YYYY-MM-DD)") String period2End) {

        log.info("Tool compare_periods called: {}-{} vs {}-{}",
                 period1Start, period1End, period2Start, period2End);

        return comparePeriodsSalesUseCase.execute(
            defaultCompanyId,
            LocalDate.parse(period1Start),
            LocalDate.parse(period1End),
            LocalDate.parse(period2Start),
            LocalDate.parse(period2End)
        );
    }

    @Tool(name = "get_payment_summary",
          description = "Obtiene resumen de ventas por metodo de pago. " +
                        "Incluye efectivo, tarjetas, transferencias, etc.")
    public List<PaymentSummary> getPaymentSummary(
            @ToolParam(description = "Codigo del local (opcional, vacio para todos)") String storeCode,
            @ToolParam(description = "Fecha inicio en formato YYYY-MM-DD") String startDate,
            @ToolParam(description = "Fecha fin en formato YYYY-MM-DD") String endDate) {

        log.info("Tool get_payment_summary called: store={}, period={} to {}",
                 storeCode, startDate, endDate);

        return getPaymentSummaryUseCase.execute(
            defaultCompanyId,
            storeCode,
            LocalDate.parse(startDate),
            LocalDate.parse(endDate)
        );
    }

    @Tool(name = "get_card_brand_summary",
          description = "Obtiene resumen de ventas por marca de tarjeta. " +
                        "Incluye Visa, Mastercard, etc. con transacciones y descuentos IVA.")
    public List<CardBrandSummary> getCardBrandSummary(
            @ToolParam(description = "Codigo del local (opcional, vacio para todos)") String storeCode,
            @ToolParam(description = "Fecha inicio en formato YYYY-MM-DD") String startDate,
            @ToolParam(description = "Fecha fin en formato YYYY-MM-DD") String endDate) {

        log.info("Tool get_card_brand_summary called: store={}, period={} to {}",
                 storeCode, startDate, endDate);

        return getCardBrandSummaryUseCase.execute(
            defaultCompanyId,
            storeCode,
            LocalDate.parse(startDate),
            LocalDate.parse(endDate)
        );
    }
}
