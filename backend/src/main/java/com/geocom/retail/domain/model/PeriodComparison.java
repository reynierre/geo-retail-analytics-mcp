package com.geocom.retail.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Value Object representing a comparison between two periods.
 */
public record PeriodComparison(
    LocalDate period1Start,
    LocalDate period1End,
    LocalDate period2Start,
    LocalDate period2End,
    BigDecimal period1Sales,
    BigDecimal period2Sales,
    long period1Tickets,
    long period2Tickets,
    BigDecimal salesVariationPercent,
    BigDecimal ticketVariationPercent
) {}
