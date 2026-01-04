package com.geocom.retail.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Value Object representing hourly traffic data.
 */
public record HourlyTraffic(
    LocalDate date,
    int hour,
    long ticketCount,
    BigDecimal totalSales
) {}
