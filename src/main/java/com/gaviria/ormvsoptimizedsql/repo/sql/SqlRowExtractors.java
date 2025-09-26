package com.gaviria.ormvsoptimizedsql.repo.sql;

import org.springframework.jdbc.core.ResultSetExtractor;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class SqlRowExtractors {

    private SqlRowExtractors() {
    }

    public static ResultSetExtractor<Map<Long, Map<String, BigDecimal>>> perAccountCurrencyTotals() {
        return rs -> {
            Map<Long, Map<String, BigDecimal>> map = new LinkedHashMap<>();
            while (rs.next()) {
                long accountId = rs.getLong("account_id");
                String currency = rs.getString("currency");
                BigDecimal totalCop = rs.getBigDecimal("total_cop");

                map.computeIfAbsent(accountId, k -> new LinkedHashMap<>())
                        .merge(currency, totalCop, BigDecimal::add);
            }
            return map;
        };
    }

    public static Set<String> pgTextArrayToSet(java.sql.Array arr) throws SQLException {
        if (arr == null) return Set.of();
        String[] raw = (String[]) arr.getArray();
        return new LinkedHashSet<>(Arrays.asList(raw));
    }
}