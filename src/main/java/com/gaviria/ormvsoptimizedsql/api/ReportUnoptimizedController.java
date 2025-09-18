package com.gaviria.ormvsoptimizedsql.api;

import com.gaviria.ormvsoptimizedsql.api.dto.CompanyConsolidatedSummaryDTO;
import com.gaviria.ormvsoptimizedsql.api.dto.CompanyMonthlySummaryDTO;
import com.gaviria.ormvsoptimizedsql.api.dto.TopCounterpartyDTO;
import com.gaviria.ormvsoptimizedsql.service.ReportUnoptimizedService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/report/orm/unoptimized")
@RequiredArgsConstructor
public class ReportUnoptimizedController {

    private final ReportUnoptimizedService service;

    @GetMapping("/summary")
    public CompanyMonthlySummaryDTO summary(
            @RequestParam Long companyId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return service.monthlySummaryPerAccount(companyId, year, month);
    }

    @GetMapping("/consolidated")
    public CompanyConsolidatedSummaryDTO consolidated(
            @RequestParam Long companyId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return service.consolidatedMonthlySummary(companyId, year, month);
    }

    @GetMapping("/top-counterparties")
    public List<TopCounterpartyDTO> topCounterparties(
            @RequestParam Long companyId,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(defaultValue = "10") int topN
    ) {
        return service.topCounterparties(companyId, year, month, topN);
    }
}