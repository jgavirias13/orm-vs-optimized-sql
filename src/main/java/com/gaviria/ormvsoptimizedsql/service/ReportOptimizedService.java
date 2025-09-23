package com.gaviria.ormvsoptimizedsql.service;

import com.gaviria.ormvsoptimizedsql.api.dto.AccountMonthlySummaryDTO;
import com.gaviria.ormvsoptimizedsql.api.dto.CompanyMonthlySummaryDTO;
import com.gaviria.ormvsoptimizedsql.api.dto.CurrencyTotalDTO;
import com.gaviria.ormvsoptimizedsql.repo.ExchangeRateRepository;
import com.gaviria.ormvsoptimizedsql.repo.MovementRepository;
import com.gaviria.ormvsoptimizedsql.repo.projection.AggMovementDayView;
import com.gaviria.ormvsoptimizedsql.repo.projection.RateRowJpql;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportOptimizedService {

    private final MovementRepository movementRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    public CompanyMonthlySummaryDTO monthlySummaryPerAccountOptimized(Long companyId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1);
        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to = endDate.atStartOfDay();

        List<AggMovementDayView> agg = movementRepository.aggregateByAccountCurrencyDay(companyId, from, to);
        if (agg.isEmpty()) {
            return new CompanyMonthlySummaryDTO(companyId, year, month, List.of(), BigDecimal.ZERO);
        }

        Set<String> currencies = agg.stream().map(AggMovementDayView::getCurrency).collect(Collectors.toSet());
        currencies.remove("COP");

        Map<String, Map<LocalDate, BigDecimal>> rateMap = new HashMap<>();
        if (!currencies.isEmpty()) {
            List<RateRowJpql> rows = exchangeRateRepository.findRatesForRangeAndCurrencies(
                    startDate, endDate, currencies
            );

            Map<String, Map<LocalDate, RateRowJpql>> best = new HashMap<>();
            for (RateRowJpql r : rows) {
                best.computeIfAbsent(r.currency(), k -> new HashMap<>())
                        .merge(r.day(), r, (a, b) -> a.version() >= b.version() ? a : b);
            }

            for (var e : best.entrySet()) {
                Map<LocalDate, BigDecimal> perDay = new HashMap<>();
                for (var d : e.getValue().entrySet()) {
                    perDay.put(d.getKey(), d.getValue().rate());
                }
                rateMap.put(e.getKey(), perDay);
            }
        }


        Map<Long, Map<String, BigDecimal>> accOrig = new HashMap<>();
        Map<Long, Map<String, BigDecimal>> accCop = new HashMap<>();

        for (AggMovementDayView a : agg) {
            Long accId = a.getAccountId();
            String ccy = a.getCurrency();
            BigDecimal sumOriginal = a.getTotalAmount();

            BigDecimal rate = BigDecimal.ONE;
            if (!"COP".equalsIgnoreCase(ccy)) {
                rate = Optional.ofNullable(rateMap.get(ccy))
                        .map(m -> m.get(a.getDay()))
                        .orElse(BigDecimal.ONE);
            }
            BigDecimal sumCop = sumOriginal.multiply(rate);

            accOrig.computeIfAbsent(accId, k -> new HashMap<>())
                    .merge(ccy, sumOriginal, BigDecimal::add);
            accCop.computeIfAbsent(accId, k -> new HashMap<>())
                    .merge(ccy, sumCop, BigDecimal::add);
        }

        List<AccountMonthlySummaryDTO> accounts = new ArrayList<>();
        BigDecimal companyTotalCop = BigDecimal.ZERO;

        for (var entry : accCop.entrySet()) {
            Long accId = entry.getKey();
            Map<String, BigDecimal> byCcyCop = entry.getValue();
            Map<String, BigDecimal> byCcyOrig = accOrig.getOrDefault(accId, Map.of());

            var byCurrency = byCcyCop.keySet().stream()
                    .sorted()
                    .map(ccy -> new CurrencyTotalDTO(
                            ccy,
                            byCcyOrig.getOrDefault(ccy, BigDecimal.ZERO),
                            byCcyCop.getOrDefault(ccy, BigDecimal.ZERO)
                    ))
                    .toList();

            BigDecimal accTotal = byCurrency.stream()
                    .map(CurrencyTotalDTO::totalCOP)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            accounts.add(new AccountMonthlySummaryDTO(accId, byCurrency, accTotal));
            companyTotalCop = companyTotalCop.add(accTotal);
        }

        accounts = accounts.stream()
                .sorted(Comparator.comparing(AccountMonthlySummaryDTO::accountId))
                .toList();

        return new CompanyMonthlySummaryDTO(companyId, year, month, accounts, companyTotalCop);
    }
}