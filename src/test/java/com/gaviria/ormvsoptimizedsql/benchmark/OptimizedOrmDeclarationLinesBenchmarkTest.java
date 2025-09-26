// src/test/java/com/gaviria/ormvsoptimizedsql/benchmark/OptimizedOrmDeclarationLinesBenchmarkTest.java
package com.gaviria.ormvsoptimizedsql.benchmark;

import com.gaviria.ormvsoptimizedsql.api.dto.DeclarationLineDTO;
import com.gaviria.ormvsoptimizedsql.service.ReportOptimizedService;
import net.ttddyy.dsproxy.QueryCountHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@ActiveProfiles("test")
@SpringBootTest
class OptimizedOrmDeclarationLinesBenchmarkTest {

    @Autowired
    ReportOptimizedService service;

    private long runOnce(Long accId, int year, int month, int page, int size) {
        QueryCountHolder.clear();
        long t0 = System.nanoTime();

        Page<DeclarationLineDTO> out = service.declarationLinesOptimized(accId, year, month, page, size);

        long t1 = System.nanoTime();

        Assertions.assertNotNull(out);
        Assertions.assertFalse(out.getContent().isEmpty());

        var qc = QueryCountHolder.getGrandTotal();
        System.out.printf(
                "Optimized C p=%d s=%d -> Time: %.3fs | Queries total=%d select=%d | items=%d%n",
                page, size, (t1 - t0) / 1_000_000_000.0,
                qc.getTotal(), qc.getSelect(),
                out.getContent().size()
        );
        return t1 - t0;
    }

    @Test
    void baselineOptimizedDeclarationLines() {
        Long accId = 1L;
        int year = 2025, month = 8, p = 0, s = 500;

        runOnce(accId, year, month, p, s);
        runOnce(accId, year, month, p, s);

        List<Long> times = new ArrayList<>();
        times.add(runOnce(accId, year, month, p, s));
        times.add(runOnce(accId, year, month, p, s));
        times.add(runOnce(accId, year, month, p, s));

        times.sort(Comparator.naturalOrder());
        long medianNs = times.get(1);
        System.out.printf("Optimized C median over 3 runs: %.3fs%n", medianNs / 1_000_000_000.0);

        Assertions.assertTrue(medianNs < 60_000_000_000L);
    }
}