package com.gaviria.ormvsoptimizedsql.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record AccountMonthlySummaryDTO(
        Long accountId,
        List<CurrencyTotalDTO> byCurrency,
        BigDecimal grandTotalCOP
) {
}