package com.gaviria.ormvsoptimizedsql.repo;

import com.gaviria.ormvsoptimizedsql.domain.ExchangeRate;
import com.gaviria.ormvsoptimizedsql.repo.projection.RateRowJpql;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    Optional<ExchangeRate> findTopByCurrency_CodeAndValidDateOrderByVersionDesc(String currencyCode, LocalDate validDate);

    @Query("""
      select new com.gaviria.ormvsoptimizedsql.repo.projection.RateRowJpql(
          er.currency.code,
          er.validDate,
          er.version,
          er.rate
      )
      from ExchangeRate er
      where er.validDate >= :fromDate
        and er.validDate <  :toDate
        and er.currency.code in :currencies
      order by er.currency.code, er.validDate, er.version
    """)
    List<RateRowJpql> findRatesForRangeAndCurrencies(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate")  LocalDate toDate,
            @Param("currencies") Collection<String> currencies
    );
}