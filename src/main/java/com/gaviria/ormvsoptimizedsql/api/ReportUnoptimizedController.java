package com.gaviria.ormvsoptimizedsql.api;

import com.gaviria.ormvsoptimizedsql.api.dto.CompanyMonthlySummaryDTO;
import com.gaviria.ormvsoptimizedsql.service.ReportUnoptimizedService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}