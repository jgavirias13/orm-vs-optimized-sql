package com.gaviria.ormvsoptimizedsql.api.dto;

import java.math.BigDecimal;

public record TopCounterpartyDTO(
        String counterparty,
        BigDecimal totalCOP,
        long txCount
) {
}