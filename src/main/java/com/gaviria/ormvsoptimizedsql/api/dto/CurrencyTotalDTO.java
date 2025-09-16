package com.gaviria.ormvsoptimizedsql.api.dto;

import java.math.BigDecimal;

public record CurrencyTotalDTO(
        String currency,
        BigDecimal totalOriginal,
        BigDecimal totalCOP
) {
}