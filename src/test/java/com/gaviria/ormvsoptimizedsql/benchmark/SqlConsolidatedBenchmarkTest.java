package com.gaviria.ormvsoptimizedsql.benchmark;

import com.gaviria.ormvsoptimizedsql.benchmark.config.DatasourceProxyTestConfig;
import com.gaviria.ormvsoptimizedsql.service.ReportSqlService;
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
@SpringBootTest(classes = { DatasourceProxyTestConfig.class })
class SqlConsolidatedBenchmarkTest {

    @Autowired
    ReportSqlService service;

    private long runOnce(Long companyId, int year, int month) {
        QueryCountHolder.clear();
        long t0 = System.nanoTime();
        var out = service.consolidated(companyId, year, month);
        long t1 = System.nanoTime();

        Assertions.assertNotNull(out);
        // si tu dataset lo permite:
        Assertions.assertFalse(out.accounts().isEmpty());
        Assertions.assertFalse(out.byCurrency().isEmpty());

        var qc = QueryCountHolder.getGrandTotal();
        System.out.printf(
                "SQL D -> Time: %.3fs | Queries total=%d select=%d | accounts=%d, byCurrency=%d%n",
                (t1 - t0) / 1e9, qc.getTotal(), qc.getSelect(),
                out.accounts().size(), out.byCurrency().size()
        );
        return t1 - t0;
    }

    @Test
    void benchmarkSqlConsolidated() {
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
        System.out.printf("SQL D median: %.3fs%n", medianNs / 1e9);
    }
}
