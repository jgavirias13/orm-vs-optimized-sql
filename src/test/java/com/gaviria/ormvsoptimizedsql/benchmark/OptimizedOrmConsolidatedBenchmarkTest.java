package com.gaviria.ormvsoptimizedsql.benchmark;

import com.gaviria.ormvsoptimizedsql.service.ReportOptimizedService;
import net.ttddyy.dsproxy.QueryCountHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.stream.Stream;

@ActiveProfiles("test")
@SpringBootTest
class OptimizedOrmConsolidatedBenchmarkTest {

    @Autowired
    ReportOptimizedService service;

    private long runOnce(Long companyId, int year, int month) {
        QueryCountHolder.clear();
        long t0 = System.nanoTime();
        var out = service.consolidatedMonthlySummaryOptimized(companyId, year, month);
        long t1 = System.nanoTime();

        Assertions.assertNotNull(out);
        Assertions.assertFalse(out.accounts().isEmpty());
        Assertions.assertFalse(out.byCurrency().isEmpty());

        var qc = QueryCountHolder.getGrandTotal();
        System.out.printf(
                "Optimized D -> Time: %.3fs | Queries total=%d select=%d | accounts=%d, byCurrency=%d%n",
                (t1 - t0) / 1e9, qc.getTotal(), qc.getSelect(),
                out.accounts().size(), out.byCurrency().size()
        );
        return t1 - t0;
    }

    @Test
    void baseline() {
        Long companyId = 1L;
        int year = 2025, month = 8;
        runOnce(companyId, year, month);
        runOnce(companyId, year, month); // warm-up
        var t = Stream.of(
                runOnce(companyId, year, month),
                runOnce(companyId, year, month),
                runOnce(companyId, year, month)).sorted().toList().get(1);
        System.out.printf("Optimized D median: %.3fs%n", t / 1e9);
    }
}