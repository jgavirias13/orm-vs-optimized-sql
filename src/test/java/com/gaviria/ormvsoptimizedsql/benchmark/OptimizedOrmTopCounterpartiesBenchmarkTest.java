package com.gaviria.ormvsoptimizedsql.benchmark;

import com.gaviria.ormvsoptimizedsql.api.dto.TopCounterpartyDTO;
import com.gaviria.ormvsoptimizedsql.service.ReportOptimizedService;
import net.ttddyy.dsproxy.QueryCountHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@ActiveProfiles("test")
@SpringBootTest
class OptimizedOrmTopCounterpartiesBenchmarkTest {

    @Autowired
    ReportOptimizedService service;

    private long runOnce(Long companyId, int year, int month, int topN) {
        QueryCountHolder.clear();
        long t0 = System.nanoTime();

        List<TopCounterpartyDTO> out = service.topCounterpartiesOptimized(companyId, year, month, topN);

        long t1 = System.nanoTime();

        Assertions.assertNotNull(out);
        Assertions.assertFalse(out.isEmpty());

        var qc = QueryCountHolder.getGrandTotal();
        System.out.printf(
                "Optimized B (topN=%d) -> Time: %.3fs | Queries total=%d select=%d | first=%s%n",
                topN, (t1 - t0) / 1_000_000_000.0,
                qc.getTotal(), qc.getSelect(),
                out.get(0).counterparty()
        );
        return t1 - t0;
    }

    @Test
    void baselineOptimizedTopCounterparties() {
        Long companyId = 1L;
        int year = 2025, month = 8, topN = 10;

        // Warm-ups
        runOnce(companyId, year, month, topN);
        runOnce(companyId, year, month, topN);

        // Measurements
        List<Long> times = new ArrayList<>();
        times.add(runOnce(companyId, year, month, topN));
        times.add(runOnce(companyId, year, month, topN));
        times.add(runOnce(companyId, year, month, topN));

        times.sort(Comparator.naturalOrder());
        long medianNs = times.get(1);
        System.out.printf("Optimized B median over 3 runs: %.3fs%n", medianNs / 1_000_000_000.0);

        Assertions.assertTrue(medianNs < 60_000_000_000L);
    }
}