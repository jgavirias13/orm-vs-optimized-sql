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
import java.sql.Array;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public List<TopCounterpartyDTO> fetchTopCounterparties(Long companyId, int year, int month, int topN) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1);
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.atStartOfDay();

        final String sql = """
                SELECT counterparty, SUM(total_cop) AS total_cop, SUM(tx_count) AS tx_count
                FROM (
                    -- 1) No-COP: join a fx
                    SELECT cp.display_name AS counterparty,
                           SUM(m.amount * fx.rate) AS total_cop,
                           COUNT(*) AS tx_count
                    FROM movement m
                    JOIN company_account ca ON ca.id = m.company_account_id
                    JOIN counterparty_account cpa ON cpa.id = m.counterparty_account_id
                    JOIN counterparty cp ON cp.id = cpa.counterparty_id
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
                    WHERE ca.company_id = ?              -- (1) companyId
                      AND m.booked_at  >= ?              -- (2) from_ts
                      AND m.booked_at  <  ?              -- (3) to_ts
                      AND m.currency_code <> 'COP'
                    GROUP BY cp.display_name
                
                    UNION ALL
                
                    -- 2) COP: sin join a fx
                    SELECT cp.display_name AS counterparty,
                           SUM(m.amount) AS total_cop,
                           COUNT(*) AS tx_count
                    FROM movement m
                    JOIN company_account ca ON ca.id = m.company_account_id
                    JOIN counterparty_account cpa ON cpa.id = m.counterparty_account_id
                    JOIN counterparty cp ON cp.id = cpa.counterparty_id
                    WHERE ca.company_id = ?              -- (6) companyId
                      AND m.booked_at  >= ?              -- (7) from_ts
                      AND m.booked_at  <  ?              -- (8) to_ts
                      AND m.currency_code = 'COP'
                    GROUP BY cp.display_name
                ) s
                GROUP BY counterparty
                ORDER BY total_cop DESC
                LIMIT ?                                    -- (9) topN
                """;

        Object[] params = {
                Date.valueOf(start),           // (4)
                Date.valueOf(end),             // (5)
                companyId,                     // (1)
                Timestamp.valueOf(from),       // (2)
                Timestamp.valueOf(to),         // (3)
                companyId,                     // (6)
                Timestamp.valueOf(from),       // (7)
                Timestamp.valueOf(to),         // (8)
                topN                           // (9)
        };

        List<TopCounterpartyDTO> out = new ArrayList<>();
        jdbc.query(sql, rs -> {
            String cp = rs.getString("counterparty");
            BigDecimal totalCop = rs.getBigDecimal("total_cop");
            long txCount = rs.getLong("tx_count");
            out.add(new TopCounterpartyDTO(cp, totalCop, txCount));
        }, params);

        return out;
    }

    // =========================
    // C) Declaration Lines (paged) + tags
    // =========================
    public Page<DeclarationLineDTO> fetchDeclarationLines(
            Long companyAccountId, int year, int month, int page, int size) {

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1);
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.atStartOfDay();

        // -------- total elements (para Page) --------
        final String countSql = """
                SELECT COUNT(*) AS total
                FROM movement m
                WHERE m.company_account_id = ?
                  AND m.booked_at >= ? AND m.booked_at < ?
                """;

        long totalElements = jdbc.queryForObject(
                countSql,
                new Object[]{companyAccountId, Timestamp.valueOf(from), Timestamp.valueOf(to)},
                Long.class
        );

        if (totalElements == 0) {
            return Page.empty(PageRequest.of(page, size));
        }

        int offset = Math.max(page, 0) * Math.max(size, 1);

        // -------- página con líneas + tags + tasa aplicada --------
        final String sql = """
                WITH page AS (
                    SELECT m.id
                    FROM movement m
                    WHERE m.company_account_id = ?
                      AND m.booked_at >= ? AND m.booked_at < ?
                    ORDER BY m.booked_at ASC, m.id ASC
                    OFFSET ? LIMIT ?
                ),
                mv AS (
                    SELECT m.id,
                           m.booked_at,
                           m.amount,
                           m.currency_code,
                           m.description,
                           cpa.counterparty_id
                    FROM movement m
                    JOIN page p ON p.id = m.id
                    JOIN counterparty_account cpa ON cpa.id = m.counterparty_account_id
                ),
                fx AS (
                    SELECT DISTINCT ON (er.currency_code, er.valid_date)
                           er.currency_code,
                           er.valid_date,
                           er.rate
                    FROM exchange_rate er
                    WHERE er.valid_date >= ?::date
                      AND er.valid_date <  ?::date
                    ORDER BY er.currency_code, er.valid_date, er.version DESC
                )
                SELECT
                    mv.booked_at::date                              AS day,
                    cp.display_name                                 AS counterparty,
                    mv.currency_code                                AS currency,
                    mv.amount                                       AS original,
                    COALESCE(
                        CASE WHEN mv.currency_code = 'COP'
                             THEN 1
                             ELSE fx.rate
                        END, 1
                    )                                               AS rate,
                    (mv.amount * COALESCE(CASE WHEN mv.currency_code = 'COP' THEN 1 ELSE fx.rate END, 1))
                                                                    AS cop,
                    mv.description                                  AS description,
                    COALESCE(array_agg(DISTINCT t.name) FILTER (WHERE t.name IS NOT NULL), '{}') AS tags
                FROM mv
                JOIN counterparty cp ON cp.id = mv.counterparty_id
                LEFT JOIN fx
                       ON fx.currency_code = mv.currency_code
                      AND fx.valid_date    = mv.booked_at::date
                LEFT JOIN movement_tags mt ON mt.movement_id = mv.id
                LEFT JOIN movement_tag  t  ON t.id = mt.tag_id
                GROUP BY mv.id, mv.booked_at, mv.amount, mv.currency_code, mv.description, cp.display_name, fx.rate
                ORDER BY mv.booked_at ASC, mv.id ASC
                """;

        Object[] params = {
                companyAccountId,                    // (1)
                Timestamp.valueOf(from),             // (2)
                Timestamp.valueOf(to),               // (3)
                offset,                              // (4)
                size,                                // (5)
                Date.valueOf(start),                 // (6) start::date
                Date.valueOf(end)                    // (7) end::date
        };

        List<DeclarationLineDTO> content = new ArrayList<>(size);
        jdbc.query(sql, rs -> {
            LocalDate day = rs.getDate("day").toLocalDate();
            String counterparty = rs.getString("counterparty");
            String currency = rs.getString("currency");
            BigDecimal original = rs.getBigDecimal("original");
            BigDecimal rate = rs.getBigDecimal("rate");
            BigDecimal cop = rs.getBigDecimal("cop");
            String description = rs.getString("description");

            Set<String> tags = Set.of();
            Array arr = rs.getArray("tags");
            if (arr != null) {
                String[] vals = (String[]) arr.getArray();
                if (vals != null && vals.length > 0) {
                    // LinkedHashSet para orden estable (si lo quieres)
                    tags = new LinkedHashSet<>(Arrays.asList(vals));
                }
            }

            content.add(new DeclarationLineDTO(
                    day, counterparty, currency, original, rate, cop, description, tags
            ));
        }, params);

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