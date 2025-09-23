package com.gaviria.ormvsoptimizedsql.repo.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AggMovementDay(
        Long accountId,
        String currency,
        LocalDate day,
        BigDecimal totalAmount
) {}