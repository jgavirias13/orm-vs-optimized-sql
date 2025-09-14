package com.gaviria.ormvsoptimizedsql.repo;

import com.gaviria.ormvsoptimizedsql.domain.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
}