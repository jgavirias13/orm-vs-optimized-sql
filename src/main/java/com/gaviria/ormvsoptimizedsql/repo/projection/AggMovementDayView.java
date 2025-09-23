package com.gaviria.ormvsoptimizedsql.repo.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface AggMovementDayView {
    Long getAccountId();

    String getCurrency();

    LocalDate getDay();

    BigDecimal getTotalAmount();
}