package com.gaviria.ormvsoptimizedsql.benchmark;

import com.gaviria.ormvsoptimizedsql.api.dto.CompanyMonthlySummaryDTO;
import com.gaviria.ormvsoptimizedsql.benchmark.config.DatasourceProxyTestConfig;
import com.gaviria.ormvsoptimizedsql.service.ReportUnoptimizedService;
import net.ttddyy.dsproxy.QueryCountHolder;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;


@ActiveProfiles("test")
@SpringBootTest(classes = { DatasourceProxyTestConfig.class })
class UnoptimizedOrmSummaryBenchmarkTest {

    @Autowired
    ReportUnoptimizedService service;
    @Autowired
    DataSource dataSource;

    private long runOnce(Long companyId, int year, int month) {
        QueryCountHolder.clear();
        long t0 = System.nanoTime();
        CompanyMonthlySummaryDTO out = service.monthlySummaryPerAccount(companyId, year, month);
        long t1 = System.nanoTime();

        Assertions.assertNotNull(out);
        Assertions.assertEquals(year, out.year());
        Assertions.assertEquals(month, out.month());
        Assertions.assertFalse(out.accounts().isEmpty());

        var qc = QueryCountHolder.getGrandTotal();
        System.out.printf("Time: %.2fs, Queries: total=%d, select=%d, insert=%d, update=%d, delete=%d%n",
                (t1 - t0) / 1_000_000_000.0,
                qc.getTotal(), qc.getSelect(), qc.getInsert(), qc.getUpdate(), qc.getDelete());

        return t1 - t0;
    }

    @Test
    void scenarioA_unoptimized_baseline() {
        System.out.println(">> DS class = " + dataSource.getClass().getName());
        Long companyId = 1L;
        int year = 2025;
        int month = 8;

        // Warm-ups
        runOnce(companyId, year, month);
        runOnce(companyId, year, month);

        List<Long> times = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            times.add(runOnce(companyId, year, month));
        }

        times.sort(Long::compareTo);
        long medianNs = times.get(1);
        System.out.printf("Median over 3 runs: %.2fs%n", medianNs / 1_000_000_000.0);

        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(120), () -> {
        });
    }
}