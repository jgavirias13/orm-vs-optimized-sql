package com.gaviria.ormvsoptimizedsql.benchmark;

import com.gaviria.ormvsoptimizedsql.benchmark.config.DatasourceProxyTestConfig;
import com.gaviria.ormvsoptimizedsql.service.ReportUnoptimizedService;
import net.ttddyy.dsproxy.QueryCountHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.stream.Stream;

@ActiveProfiles("test")
@SpringBootTest(classes = { DatasourceProxyTestConfig.class })
public class UnoptimizedOrmTopCounterpartiesBenchmarkTest {
    @Autowired
    ReportUnoptimizedService service;

    private long runOnce(Long companyId, int year, int month, int topN) {
        QueryCountHolder.clear();
        long t0 = System.nanoTime();
        var out = service.topCounterparties(companyId, year, month, topN);
        long t1 = System.nanoTime();

        Assertions.assertFalse(out.isEmpty());
        var qc = QueryCountHolder.getGrandTotal();
        System.out.printf("TopN=%d -> Time: %.2fs | Queries total=%d select=%d%n",
                topN, (t1 - t0) / 1e9, qc.getTotal(), qc.getSelect());
        return t1 - t0;
    }

    @Test
    void baseline() {
        Long companyId = 1L;
        int y = 2025, m = 8, topN = 10;
        runOnce(companyId, y, m, topN); // warm-up
        runOnce(companyId, y, m, topN); // warm-up
        var t1 = runOnce(companyId, y, m, topN);
        var t2 = runOnce(companyId, y, m, topN);
        var t3 = runOnce(companyId, y, m, topN);
        var median = Stream.of(t1, t2, t3).sorted().skip(1).findFirst().orElse(t2);
        System.out.printf("Median: %.2fs%n", median / 1e9);
    }
}
