package com.gaviria.ormvsoptimizedsql.service;

import com.gaviria.ormvsoptimizedsql.api.dto.AccountMonthlySummaryDTO;
import com.gaviria.ormvsoptimizedsql.api.dto.CompanyMonthlySummaryDTO;
import com.gaviria.ormvsoptimizedsql.api.dto.CurrencyTotalDTO;
import com.gaviria.ormvsoptimizedsql.domain.ExchangeRate;
import com.gaviria.ormvsoptimizedsql.domain.Movement;
import com.gaviria.ormvsoptimizedsql.repo.CompanyAccountRepository;
import com.gaviria.ormvsoptimizedsql.repo.ExchangeRateRepository;
import com.gaviria.ormvsoptimizedsql.repo.MovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportUnoptimizedService {

    private final CompanyAccountRepository companyAccountRepository;
    private final MovementRepository movementRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    // Unoptimized version
    public CompanyMonthlySummaryDTO monthlySummaryPerAccount(Long companyId, int year, int month) {
        var accounts = companyAccountRepository.findCompanyAccountByCompany_Id(companyId);

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1);
        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to = endDate.atStartOfDay();

        List<AccountMonthlySummaryDTO> accountSummaries = new ArrayList<>();
        BigDecimal companyTotalCop = BigDecimal.ZERO;

        for (var acc : accounts) {
            Map<String, BigDecimal> totalsOriginal = new HashMap<>();
            Map<String, BigDecimal> totalsCop = new HashMap<>();

            int page = 0;
            int size = 1000;
            while (true) {
                Page<Movement> movements = movementRepository.findByCompanyAccountAndPeriod(
                        acc.getId(), from, to, PageRequest.of(page, size)
                );
                if (movements.isEmpty()) break;

                for (Movement m : movements.getContent()) {
                    String currency = m.getCurrency().getCode();
                    BigDecimal original = m.getAmount();

                    BigDecimal rate = BigDecimal.ONE;
                    if (!"COP".equalsIgnoreCase(currency)) {
                        var bookedDay = m.getBookedAt().toLocalDate();
                        var optRate = exchangeRateRepository
                                .findTopByCurrency_CodeAndValidDateOrderByVersionDesc(currency, bookedDay);

                        rate = optRate.map(ExchangeRate::getRate).orElse(BigDecimal.ONE);
                    }

                    BigDecimal cop = original.multiply(rate);

                    totalsOriginal.merge(currency, original, BigDecimal::add);
                    totalsCop.merge(currency, cop, BigDecimal::add);
                }

                if (!movements.hasNext()) break;
                page++;
            }

            var byCurrency = totalsOriginal.keySet().stream()
                    .sorted()
                    .map(ccy -> new CurrencyTotalDTO(
                            ccy,
                            totalsOriginal.getOrDefault(ccy, BigDecimal.ZERO),
                            totalsCop.getOrDefault(ccy, BigDecimal.ZERO)
                    ))
                    .collect(Collectors.toList());

            BigDecimal accountTotalCop = byCurrency.stream()
                    .map(CurrencyTotalDTO::totalCOP)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            accountSummaries.add(new AccountMonthlySummaryDTO(
                    acc.getId(), byCurrency, accountTotalCop
            ));

            companyTotalCop = companyTotalCop.add(accountTotalCop);
        }

        return new CompanyMonthlySummaryDTO(
                companyId, year, month, accountSummaries, companyTotalCop
        );
    }
}