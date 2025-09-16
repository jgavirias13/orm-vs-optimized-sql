package com.gaviria.ormvsoptimizedsql.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record CompanyMonthlySummaryDTO(
        Long companyId,
        int year,
        int month,
        List<AccountMonthlySummaryDTO> accounts,
        BigDecimal companyTotalCOP
) {
}