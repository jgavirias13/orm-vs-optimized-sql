package com.gaviria.ormvsoptimizedsql.repo.sql;

import com.gaviria.ormvsoptimizedsql.api.dto.AccountCopTotalDTO;
import com.gaviria.ormvsoptimizedsql.api.dto.AccountMonthlySummaryDTO;
import com.gaviria.ormvsoptimizedsql.api.dto.CompanyConsolidatedSummaryDTO;
import com.gaviria.ormvsoptimizedsql.api.dto.CompanyMonthlySummaryDTO;
import com.gaviria.ormvsoptimizedsql.api.dto.CurrencyTotalDTO;
import com.gaviria.ormvsoptimizedsql.api.dto.DeclarationLineDTO;
import com.gaviria.ormvsoptimizedsql.api.dto.TopCounterpartyDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class SqlReportDao {

    private final JdbcTemplate jdbc;

    // =========================
    // A) Monthly Summary by Account
    // =========================
    public CompanyMonthlySummaryDTO fetchMonthlySummary(Long companyId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1);
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.atStartOfDay();

        final String sqlPerAccountCurrency = """
                SELECT account_id,
                       currency,
                       SUM(total_original) AS total_original,
                       SUM(total_cop)      AS total_cop
                FROM (
                  -- 1) No-COP: join a fx
                  SELECT m.company_account_id AS account_id,
                         m.currency_code      AS currency,
                         SUM(m.amount)        AS total_original,
                         SUM(m.amount * fx.rate) AS total_cop
                  FROM movement m
                  JOIN company_account ca ON ca.id = m.company_account_id
                  JOIN (
                    SELECT DISTINCT ON (er.currency_code, er.valid_date)
                           er.currency_code, er.valid_date, er.rate
                    FROM exchange_rate er
                    WHERE er.valid_date >= ?::date   -- (4) start-date
                      AND er.valid_date <  ?::date   -- (5) end-date
                    ORDER BY er.currency_code, er.valid_date, er.version DESC
                  ) fx
                    ON fx.currency_code = m.currency_code
                   AND fx.valid_date    = m.booked_at::date
                  WHERE ca.company_id = ?            -- (1) companyId
                    AND m.booked_at  >= ?            -- (2) from_ts
                    AND m.booked_at  <  ?            -- (3) to_ts
                    AND m.currency_code <> 'COP'
                  GROUP BY m.company_account_id, m.currency_code
                
                  UNION ALL
                
                  -- 2) COP: sin join a fx
                  SELECT m.company_account_id AS account_id,
                         m.currency_code      AS currency,
                         SUM(m.amount)        AS total_original,
                         SUM(m.amount)        AS total_cop
                  FROM movement m
                  JOIN company_account ca ON ca.id = m.company_account_id
                  WHERE ca.company_id = ?            -- (6) companyId
                    AND m.booked_at  >= ?            -- (7) from_ts
                    AND m.booked_at  <  ?            -- (8) to_ts
                    AND m.currency_code = 'COP'
                  GROUP BY m.company_account_id, m.currency_code
                ) s
                GROUP BY account_id, currency
                ORDER BY account_id, currency
                """;

        Map<Long, Map<String, BigDecimal>> byAccOrig = new LinkedHashMap<>();
        Map<Long, Map<String, BigDecimal>> byAccCop = new LinkedHashMap<>();

        Object[] params = {
                Date.valueOf(start),
                Date.valueOf(end),
                companyId,
                Timestamp.valueOf(from),
                Timestamp.valueOf(to),
                companyId,
                Timestamp.valueOf(from),
                Timestamp.valueOf(to)
        };

        jdbc.query(sqlPerAccountCurrency, rs -> {
            long accountId = rs.getLong("account_id");
            String currency = rs.getString("currency");
            BigDecimal totalOriginal = rs.getBigDecimal("total_original");
            BigDecimal totalCop = rs.getBigDecimal("total_cop");

            byAccOrig.computeIfAbsent(accountId, k -> new LinkedHashMap<>())
                    .merge(currency, totalOriginal, BigDecimal::add);

            byAccCop.computeIfAbsent(accountId, k -> new LinkedHashMap<>())
                    .merge(currency, totalCop, BigDecimal::add);
        }, params);

        var accountSummaries = new ArrayList<AccountMonthlySummaryDTO>();
        BigDecimal companyTotalCop = BigDecimal.ZERO;

        for (var entry : byAccOrig.entrySet()) {
            Long accId = entry.getKey();
            Map<String, BigDecimal> origs = entry.getValue();
            Map<String, BigDecimal> cops = byAccCop.getOrDefault(accId, Map.of());

            var byCurrency = origs.keySet().stream()
                    .sorted()
                    .map(ccy -> new CurrencyTotalDTO(
                            ccy,
                            origs.getOrDefault(ccy, BigDecimal.ZERO),
                            cops.getOrDefault(ccy, BigDecimal.ZERO)
                    ))
                    .toList();

            var accTotal = byCurrency.stream()
                    .map(CurrencyTotalDTO::totalCOP)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            accountSummaries.add(new AccountMonthlySummaryDTO(accId, byCurrency, accTotal));
            companyTotalCop = companyTotalCop.add(accTotal);
        }

        if (accountSummaries.isEmpty()) {
            return new CompanyMonthlySummaryDTO(companyId, year, month, List.of(), BigDecimal.ZERO);
        }

        return new CompanyMonthlySummaryDTO(
                companyId,
                year,
                month,
                accountSummaries,
                companyTotalCop
        );
    }

    // =========================
    // B) Top Counterparties
    // =========================
    public List<TopCounterpartyDTO> fetchTopCounterparties(Long companyId, int year, int month, int topN) throws DataAccessException {
        // TODO SQL:
        // WITH mv AS (...),
        //      fx AS (...),
        // SELECT cp.display_name, SUM(amount * rate) AS total_cop, COUNT(*) AS tx_count
        // FROM mv JOIN ... GROUP BY cp.display_name ORDER BY total_cop DESC LIMIT :topN

        String sql = "/* TODO: fill SQL for scenario B */ SELECT 1";

        return List.of();
    }

    // =========================
    // C) Declaration Lines (paged) + tags
    // =========================
    public Page<DeclarationLineDTO> fetchDeclarationLines(Long companyAccountId, int year, int month, int page, int size) throws DataAccessException {
        // TODO SQL:
        // WITH page AS (... ids de movimiento ordenados con OFFSET/LIMIT ...),
        //      mv AS (... movimientos de la página ...),
        //      fx AS (...),
        // SELECT m.booked_at, cp.display_name, cur.code, m.amount, rate,
        //        (m.amount * COALESCE(rate,1)) AS cop, m.description,
        //        COALESCE(array_agg(DISTINCT t.name) FILTER (WHERE t.name IS NOT NULL), '{}') AS tags
        // FROM mv m LEFT JOIN movement_tags ... GROUP BY ... ORDER BY ...

        String sql = "/* TODO: fill SQL for scenario C */ SELECT 1";

        String countSql = "/* TODO: count total movements in period for this account */ SELECT 0";

        // Placeholder
        List<DeclarationLineDTO> content = List.of();
        long totalElements = 0;

        Pageable pageable = PageRequest.of(page, size);
        return new PageImpl<>(content, pageable, totalElements);
    }

    // =========================
    // D) Consolidated Monthly Summary
    // =========================
    public CompanyConsolidatedSummaryDTO fetchConsolidated(Long companyId, int year, int month) throws DataAccessException {
        // TODO SQL:
        // WITH mv AS (...), fx AS (...)
        // 1) per-account total COP
        // 2) per-currency total original + total COP
        // 3) grand total
        // Puedes hacer 2-3 SELECTs dentro de una misma llamada (ver abajo) o ejecutar 2-3 queries separadas.

        String sqlAccounts = "/* TODO: accounts total COP */ SELECT 1";
        String sqlCurrencies = "/* TODO: by currency original+cop */ SELECT 1";
        String sqlGrandTotal = "/* TODO: grand total */ SELECT 0";

        List<AccountCopTotalDTO> accounts = List.of();
        List<CurrencyTotalDTO> byCurrency = List.of();
        BigDecimal grandTotal = BigDecimal.ZERO;

        return new CompanyConsolidatedSummaryDTO(
                companyId,
                String.format("%04d-%02d", year, month),
                accounts,
                byCurrency,
                grandTotal
        );
    }
}