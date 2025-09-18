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
class UnoptimizedOrmDeclarationLinesBenchmarkTest {
    @Autowired
    ReportUnoptimizedService service;

    private long runOnce(Long accId, int year, int month, int page, int size) {
        QueryCountHolder.clear();
        long t0 = System.nanoTime();
        var out = service.declarationLines(accId, year, month, page, size);
        long t1 = System.nanoTime();

        Assertions.assertFalse(out.getContent().isEmpty());
        var qc = QueryCountHolder.getGrandTotal();
        System.out.printf("Lines p=%d s=%d -> Time: %.2fs | Queries total=%d select=%d%n",
                page, size, (t1-t0)/1e9, qc.getTotal(), qc.getSelect());
        return t1 - t0;
    }

    @Test
    void baseline_page0() {
        Long accId = 1L; int y=2025, m=8, p=0, s=500;
        runOnce(accId,y,m,p,s); // warm-up
        var t1 = runOnce(accId,y,m,p,s);
        var t2 = runOnce(accId,y,m,p,s);
        var t3 = runOnce(accId,y,m,p,s);
        var median = Stream.of(t1,t2,t3).sorted().skip(1).findFirst().orElse(t2);
        System.out.printf("Median: %.2fs%n", median/1e9);
    }
}