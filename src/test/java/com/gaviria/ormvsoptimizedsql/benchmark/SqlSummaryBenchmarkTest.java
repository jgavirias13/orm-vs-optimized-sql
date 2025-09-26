package com.gaviria.ormvsoptimizedsql.benchmark;

import com.gaviria.ormvsoptimizedsql.api.dto.CompanyMonthlySummaryDTO;
import com.gaviria.ormvsoptimizedsql.service.ReportSqlService;
import net.ttddyy.dsproxy.QueryCount;
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
class SqlSummaryBenchmarkTest {

    @Autowired
    ReportSqlService service;

    private long runOnce(Long companyId, int year, int month) {
        QueryCountHolder.clear();
        long t0 = System.nanoTime();

        CompanyMonthlySummaryDTO out = service.monthlySummary(companyId, year, month);

        long t1 = System.nanoTime();

        // sanity checks
        Assertions.assertNotNull(out);
        Assertions.assertEquals(year, out.year());
        Assertions.assertEquals(month, out.month());
        // con el seed normal debería haber cuentas; si tu seed está vacío, ajusta este assert
        Assertions.assertFalse(out.accounts().isEmpty(), "Expected at least one account in monthly summary");

        QueryCount qc = QueryCountHolder.getGrandTotal();
        System.out.printf(
                "SQL A -> Time: %.3fs | Queries: total=%d, select=%d, insert=%d, update=%d, delete=%d | accounts=%d%n",
                (t1 - t0) / 1_000_000_000.0,
                qc.getTotal(), qc.getSelect(), qc.getInsert(), qc.getUpdate(), qc.getDelete(),
                out.accounts().size()
        );

        return t1 - t0;
    }

    @Test
    void benchmarkSqlSummary() {
        Long companyId = 1L;
        int year = 2025, month = 8;

        // warm-ups (no considerar en estadísticas)
        runOnce(companyId, year, month);
        runOnce(companyId, year, month);

        // mediciones
        List<Long> times = new ArrayList<>();
        times.add(runOnce(companyId, year, month));
        times.add(runOnce(companyId, year, month));
        times.add(runOnce(companyId, year, month));

        times.sort(Comparator.naturalOrder());
        long medianNs = times.get(1);
        System.out.printf("SQL A median over 3 runs: %.3fs%n", medianNs / 1_000_000_000.0);

        // umbral amplio; ajústalo según tu máquina local
        Assertions.assertTrue(medianNs < 30_000_000_000L, "Median should be under 30s on dev machine");
    }
}