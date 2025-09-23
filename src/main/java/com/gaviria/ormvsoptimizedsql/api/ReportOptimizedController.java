package com.gaviria.ormvsoptimizedsql.api;

import com.gaviria.ormvsoptimizedsql.api.dto.CompanyMonthlySummaryDTO;
import com.gaviria.ormvsoptimizedsql.service.ReportOptimizedService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
}