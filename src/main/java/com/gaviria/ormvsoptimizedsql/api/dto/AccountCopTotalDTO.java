package com.gaviria.ormvsoptimizedsql.api.dto;

import java.math.BigDecimal;

public record AccountCopTotalDTO(
        Long accountId,
        BigDecimal totalCOP
) {
}