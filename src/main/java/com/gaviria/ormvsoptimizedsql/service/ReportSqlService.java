package com.gaviria.ormvsoptimizedsql.service;

import com.gaviria.ormvsoptimizedsql.api.dto.CompanyConsolidatedSummaryDTO;
import com.gaviria.ormvsoptimizedsql.api.dto.CompanyMonthlySummaryDTO;
import com.gaviria.ormvsoptimizedsql.api.dto.DeclarationLineDTO;
import com.gaviria.ormvsoptimizedsql.api.dto.TopCounterpartyDTO;
import com.gaviria.ormvsoptimizedsql.repo.sql.SqlReportDao;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportSqlService {

    private final SqlReportDao dao;

    public CompanyMonthlySummaryDTO monthlySummary(Long companyId, int year, int month) {
        return dao.fetchMonthlySummary(companyId, year, month);
    }

    public List<TopCounterpartyDTO> topCounterparties(Long companyId, int year, int month, int topN) {
        return dao.fetchTopCounterparties(companyId, year, month, topN);
    }

    public Page<DeclarationLineDTO> declarationLines(Long companyAccountId, int year, int month, int page, int size) {
        return dao.fetchDeclarationLines(companyAccountId, year, month, page, size);
    }

    public CompanyConsolidatedSummaryDTO consolidated(Long companyId, int year, int month) {
        return dao.fetchConsolidated(companyId, year, month);
    }
}