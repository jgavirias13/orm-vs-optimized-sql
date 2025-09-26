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
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SqlReportDao {

    private final JdbcTemplate jdbc;

    // =========================
    // A) Monthly Summary by Account
    // =========================
    public CompanyMonthlySummaryDTO fetchMonthlySummary(Long companyId, int year, int month) throws DataAccessException {
        // TODO SQL:
        // WITH mv AS (... movimientos del periodo con date_trunc('day', booked_at) AS d ...),
        //      fx AS (... DISTINCT ON (currency_code, valid_date) ORDER BY version DESC ...),
        // SELECT account_id, currency, SUM(amount) AS total_original, SUM(amount * COALESCE(rate,1)) AS total_cop
        // FROM ...
        // GROUP BY account_id, currency

        String sql = "/* TODO: fill SQL for scenario A (single query) */ SELECT 1";

        List<AccountMonthlySummaryDTO> accounts = List.of();
        BigDecimal companyTotal = BigDecimal.ZERO;

        return new CompanyMonthlySummaryDTO(companyId, year, month, accounts, companyTotal);
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