package com.gaviria.ormvsoptimizedsql.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DeclarationLineDTO(
        LocalDate date,
        String counterparty,
        String currency,
        BigDecimal amountOriginal,
        BigDecimal rateCOP,
        BigDecimal amountCOP,
        String description
) {
}