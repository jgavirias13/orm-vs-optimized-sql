package com.gaviria.ormvsoptimizedsql.repo.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface AggCounterpartyDayView {
    String getCounterparty();

    String getCurrency();

    LocalDate getDay();

    BigDecimal getTotalAmount();

    Long getTxCount();
}