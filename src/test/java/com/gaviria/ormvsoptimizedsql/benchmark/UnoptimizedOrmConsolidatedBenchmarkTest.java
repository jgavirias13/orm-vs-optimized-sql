package com.gaviria.ormvsoptimizedsql.benchmark;

import com.gaviria.ormvsoptimizedsql.api.dto.CompanyConsolidatedSummaryDTO;
import com.gaviria.ormvsoptimizedsql.service.ReportUnoptimizedService;
import net.ttddyy.dsproxy.QueryCountHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

@ActiveProfiles("test")
@SpringBootTest
class UnoptimizedOrmConsolidatedBenchmarkTest {

    @Autowired
    ReportUnoptimizedService service;

    private long runOnce(Long companyId, int year, int month) {
        QueryCountHolder.clear();

        long t0 = System.nanoTime();
        CompanyConsolidatedSummaryDTO out = service.consolidatedMonthlySummary(companyId, year, month);
        long t1 = System.nanoTime();

        Assertions.assertNotNull(out, "Should return a DTO");
        Assertions.assertEquals(String.format("%04d-%02d", year, month), out.period(), "Period mismatch");
        Assertions.assertFalse(out.accounts().isEmpty(), "Accounts list should not be empty");
        Assertions.assertFalse(out.byCurrency().isEmpty(), "byCurrency should not be empty");

        var qc = QueryCountHolder.getGrandTotal();
        System.out.printf(
                "Consolidated -> Time: %.2fs | Queries: total=%d, select=%d, insert=%d, update=%d, delete=%d | accounts=%d, byCurrency=%d%n",
                (t1 - t0) / 1_000_000_000.0,
                qc.getTotal(), qc.getSelect(), qc.getInsert(), qc.getUpdate(), qc.getDelete(),
                out.accounts().size(), out.byCurrency().size()
        );

        return t1 - t0;
    }

    @Test
    void scenarioD_unoptimized_consolidated_baseline() {
        Long companyId = 1L; // usa un ID válido del seed
        int year = 2025, month = 8;

        runOnce(companyId, year, month);
        runOnce(companyId, year, month);

        List<Long> times = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            times.add(runOnce(companyId, year, month));
        }

        times.sort(Long::compareTo);
        long medianNs = times.get(1);
        System.out.printf("Consolidated median over 3 runs: %.2fs%n", medianNs / 1_000_000_000.0);
    }
}