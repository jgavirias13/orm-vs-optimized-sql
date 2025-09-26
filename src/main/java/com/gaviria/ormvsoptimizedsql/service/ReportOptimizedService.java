package com.gaviria.ormvsoptimizedsql.service;

import com.gaviria.ormvsoptimizedsql.api.dto.AccountCopTotalDTO;
import com.gaviria.ormvsoptimizedsql.api.dto.AccountMonthlySummaryDTO;
import com.gaviria.ormvsoptimizedsql.api.dto.CompanyConsolidatedSummaryDTO;
import com.gaviria.ormvsoptimizedsql.api.dto.CompanyMonthlySummaryDTO;
import com.gaviria.ormvsoptimizedsql.api.dto.CurrencyTotalDTO;
import com.gaviria.ormvsoptimizedsql.api.dto.DeclarationLineDTO;
import com.gaviria.ormvsoptimizedsql.api.dto.TopCounterpartyDTO;
import com.gaviria.ormvsoptimizedsql.repo.ExchangeRateRepository;
import com.gaviria.ormvsoptimizedsql.repo.MovementRepository;
import com.gaviria.ormvsoptimizedsql.repo.projection.AggCounterpartyDayView;
import com.gaviria.ormvsoptimizedsql.repo.projection.AggMovementDayView;
import com.gaviria.ormvsoptimizedsql.repo.projection.MovementLineView;
import com.gaviria.ormvsoptimizedsql.repo.projection.MovementTagPairView;
import com.gaviria.ormvsoptimizedsql.repo.projection.RateRowJpql;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
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

    public List<TopCounterpartyDTO> topCounterpartiesOptimized(Long companyId, int year, int month, int topN) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1);
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.atStartOfDay();

        List<AggCounterpartyDayView> agg = movementRepository.aggregateByCounterpartyCurrencyDay(companyId, from, to);
        if (agg.isEmpty()) return List.of();

        Set<String> currencies = agg.stream().map(AggCounterpartyDayView::getCurrency).collect(Collectors.toSet());
        currencies.remove("COP");

        Map<String, Map<LocalDate, BigDecimal>> rateMap = new HashMap<>();
        if (!currencies.isEmpty()) {
            List<RateRowJpql> rows = exchangeRateRepository.findRatesForRangeAndCurrencies(start, end, currencies);
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

        Map<String, BigDecimal> totalCopByCp = new HashMap<>();
        Map<String, Long> txByCp = new HashMap<>();

        for (AggCounterpartyDayView a : agg) {
            String cp = a.getCounterparty();
            String ccy = a.getCurrency();
            LocalDate day = a.getDay();
            BigDecimal original = a.getTotalAmount();

            BigDecimal rate = BigDecimal.ONE;
            if (!"COP".equalsIgnoreCase(ccy)) {
                rate = Optional.ofNullable(rateMap.get(ccy))
                        .map(m -> m.get(day))
                        .orElse(BigDecimal.ONE);
            }
            BigDecimal cop = original.multiply(rate);

            totalCopByCp.merge(cp, cop, BigDecimal::add);
            txByCp.merge(cp, a.getTxCount(), Long::sum);
        }

        return totalCopByCp.entrySet().stream()
                .map(e -> new TopCounterpartyDTO(e.getKey(), e.getValue(), txByCp.getOrDefault(e.getKey(), 0L)))
                .sorted(Comparator.comparing(TopCounterpartyDTO::totalCOP).reversed())
                .limit(topN)
                .toList();
    }

    public Page<DeclarationLineDTO> declarationLinesOptimized(
            Long companyAccountId, int year, int month, int page, int size) {

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1);
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.atStartOfDay();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "bookedAt").and(Sort.by("id")));

        Page<MovementLineView> pg = movementRepository.pageLinesForAccountAndPeriod(companyAccountId, from, to, pageable);
        if (pg.isEmpty()) {
            return Page.empty(pageable);
        }

        Set<String> currencies = pg.getContent().stream()
                .map(MovementLineView::getCurrency)
                .filter(ccy -> !"COP".equalsIgnoreCase(ccy))
                .collect(Collectors.toSet());

        Map<String, Map<LocalDate, BigDecimal>> rateMap = new HashMap<>();
        if (!currencies.isEmpty()) {
            List<RateRowJpql> rows = exchangeRateRepository.findRatesForRangeAndCurrencies(start, end, currencies);
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

        List<Long> ids = pg.getContent().stream().map(MovementLineView::getId).toList();
        Map<Long, Set<String>> tagsByMovement = new HashMap<>();
        if (!ids.isEmpty()) {
            for (MovementTagPairView p : movementRepository.findTagsForMovements(ids)) {
                tagsByMovement.computeIfAbsent(p.getMovementId(), k -> new LinkedHashSet<>()).add(p.getTagName());
            }
        }

        List<DeclarationLineDTO> content = pg.getContent().stream().map(v -> {
            LocalDate day = v.getBookedAt().toLocalDate();
            String ccy = v.getCurrency();
            BigDecimal original = v.getAmount();

            BigDecimal rate = BigDecimal.ONE;
            if (!"COP".equalsIgnoreCase(ccy)) {
                rate = Optional.ofNullable(rateMap.get(ccy))
                        .map(m -> m.get(day))
                        .orElse(BigDecimal.ONE);
            }
            BigDecimal cop = original.multiply(rate);

            return new DeclarationLineDTO(
                    day,
                    v.getCounterparty(),
                    ccy,
                    original,
                    rate,
                    cop,
                    v.getDescription(),
                    tagsByMovement.getOrDefault(v.getId(), Set.of())
            );
        }).toList();

        return new PageImpl<>(content, pageable, pg.getTotalElements());
    }

    @Transactional(readOnly = true)
    public CompanyConsolidatedSummaryDTO consolidatedMonthlySummaryOptimized(Long companyId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1);
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.atStartOfDay();

        var agg = movementRepository.aggregateByAccountCurrencyDay(companyId, from, to);
        if (agg.isEmpty()) {
            return new CompanyConsolidatedSummaryDTO(
                    companyId, String.format("%04d-%02d", year, month),
                    List.of(), List.of(), BigDecimal.ZERO
            );
        }

        Set<String> currencies = agg.stream().map(AggMovementDayView::getCurrency).collect(Collectors.toSet());
        currencies.remove("COP");

        Map<String, Map<LocalDate, BigDecimal>> rateMap = new HashMap<>();
        if (!currencies.isEmpty()) {
            var rows = exchangeRateRepository.findRatesForRangeAndCurrencies(start, end, currencies);
            Map<String, Map<LocalDate, RateRowJpql>> best = new HashMap<>();
            for (var r : rows) {
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

        Map<Long, BigDecimal> perAccountCop = new HashMap<>();
        Map<String, BigDecimal> totalsOriginal = new HashMap<>();
        Map<String, BigDecimal> totalsCop = new HashMap<>();

        for (var a : agg) {
            String ccy = a.getCurrency();
            BigDecimal originalSum = a.getTotalAmount();
            LocalDate day = a.getDay();

            BigDecimal rate = BigDecimal.ONE;
            if (!"COP".equalsIgnoreCase(ccy)) {
                rate = Optional.ofNullable(rateMap.get(ccy))
                        .map(m -> m.get(day))
                        .orElse(BigDecimal.ONE);
            }
            BigDecimal cop = originalSum.multiply(rate);

            perAccountCop.merge(a.getAccountId(), cop, BigDecimal::add);
            totalsOriginal.merge(ccy, originalSum, BigDecimal::add);
            totalsCop.merge(ccy, cop, BigDecimal::add);
        }
        
        var accounts = perAccountCop.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new AccountCopTotalDTO(e.getKey(), e.getValue()))
                .toList();

        var byCurrency = totalsOriginal.keySet().stream()
                .sorted()
                .map(ccy -> new CurrencyTotalDTO(
                        ccy,
                        totalsOriginal.getOrDefault(ccy, BigDecimal.ZERO),
                        totalsCop.getOrDefault(ccy, BigDecimal.ZERO)
                ))
                .toList();

        BigDecimal grandTotal = accounts.stream()
                .map(AccountCopTotalDTO::totalCOP)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CompanyConsolidatedSummaryDTO(
                companyId,
                String.format("%04d-%02d", year, month),
                accounts,
                byCurrency,
                grandTotal
        );
    }
}