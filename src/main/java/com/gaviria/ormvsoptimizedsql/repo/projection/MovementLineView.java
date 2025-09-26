package com.gaviria.ormvsoptimizedsql.repo.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface MovementLineView {
    Long getId();

    LocalDateTime getBookedAt();

    String getCounterparty();

    String getCurrency();

    BigDecimal getAmount();

    String getDescription();
}