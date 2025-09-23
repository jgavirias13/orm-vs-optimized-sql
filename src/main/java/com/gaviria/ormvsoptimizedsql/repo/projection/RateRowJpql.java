package com.gaviria.ormvsoptimizedsql.repo.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RateRowJpql(
        String currency,
        LocalDate day,
        Integer version,
        BigDecimal rate
) {}