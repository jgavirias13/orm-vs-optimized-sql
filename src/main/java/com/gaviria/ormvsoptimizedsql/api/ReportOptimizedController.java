package com.gaviria.ormvsoptimizedsql.api;

import com.gaviria.ormvsoptimizedsql.api.dto.CompanyMonthlySummaryDTO;
import com.gaviria.ormvsoptimizedsql.api.dto.DeclarationLineDTO;
import com.gaviria.ormvsoptimizedsql.api.dto.TopCounterpartyDTO;
import com.gaviria.ormvsoptimizedsql.service.ReportOptimizedService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/report/orm/optimized")
@RequiredArgsConstructor
public class ReportOptimizedController {

    private final ReportOptimizedService service;

    @GetMapping("/summary")
    public CompanyMonthlySummaryDTO summaryOptimized(
            @RequestParam Long companyId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return service.monthlySummaryPerAccountOptimized(companyId, year, month);
    }

    @GetMapping("/top-counterparties")
    public List<TopCounterpartyDTO> topCounterpartiesOptimized(
            @RequestParam Long companyId,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(defaultValue = "10") int topN
    ) {
        return service.topCounterpartiesOptimized(companyId, year, month, topN);
    }

    @GetMapping("/declaration-lines")
    public Page<DeclarationLineDTO> declarationLinesOptimized(
            @RequestParam Long companyAccountId,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        return service.declarationLinesOptimized(companyAccountId, year, month, page, size);
    }
}