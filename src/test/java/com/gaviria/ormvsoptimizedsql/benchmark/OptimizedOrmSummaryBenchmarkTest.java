package com.gaviria.ormvsoptimizedsql.benchmark;

import com.gaviria.ormvsoptimizedsql.api.dto.CompanyMonthlySummaryDTO;
import com.gaviria.ormvsoptimizedsql.benchmark.config.DatasourceProxyTestConfig;
import com.gaviria.ormvsoptimizedsql.service.ReportOptimizedService;
import net.ttddyy.dsproxy.QueryCount;
import net.ttddyy.dsproxy.QueryCountHolder;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@ActiveProfiles("test")
@SpringBootTest(classes = { DatasourceProxyTestConfig.class })
class OptimizedOrmSummaryBenchmarkTest {

    @Autowired
    ReportOptimizedService service;

    private long runOnce(Long companyId, int year, int month) {
        QueryCountHolder.clear();
        long t0 = System.nanoTime();

        CompanyMonthlySummaryDTO out = service.monthlySummaryPerAccountOptimized(companyId, year, month);

        long t1 = System.nanoTime();

        Assertions.assertNotNull(out, "DTO must not be null");
        Assertions.assertEquals(year, out.year());
        Assertions.assertEquals(month, out.month());
        Assertions.assertFalse(out.accounts().isEmpty(), "Expected at least one account in summary");

        QueryCount qc = QueryCountHolder.getGrandTotal();
        System.out.printf(
                "Optimized A -> Time: %.3fs | Queries: total=%d, select=%d, insert=%d, update=%d, delete=%d | accounts=%d%n",
                (t1 - t0) / 1_000_000_000.0,
                qc.getTotal(), qc.getSelect(), qc.getInsert(), qc.getUpdate(), qc.getDelete(),
                out.accounts().size()
        );

        return t1 - t0;
    }

    @Test
    void baselineOptimizedSummary() {
        Long companyId = 1L;
        int year = 2025, month = 8;

        runOnce(companyId, year, month);
        runOnce(companyId, year, month);

        List<Long> times = new ArrayList<>();
        times.add(runOnce(companyId, year, month));
        times.add(runOnce(companyId, year, month));
        times.add(runOnce(companyId, year, month));

        times.sort(Comparator.naturalOrder());
        long medianNs = times.get(1);
        System.out.printf("Optimized A median over 3 runs: %.3fs%n", medianNs / 1_000_000_000.0);
    }
}