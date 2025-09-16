package com.gaviria.ormvsoptimizedsql.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record CompanyConsolidatedSummaryDTO(
        Long companyId,
        String period,
        List<AccountCopTotalDTO> accounts,
        List<CurrencyTotalDTO> byCurrency,
        BigDecimal grandTotalCOP
) {
}