package com.gaviria.ormvsoptimizedsql.repo;

import com.gaviria.ormvsoptimizedsql.domain.Counterparty;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CounterpartyRepository extends JpaRepository<Counterparty, Long> {
}