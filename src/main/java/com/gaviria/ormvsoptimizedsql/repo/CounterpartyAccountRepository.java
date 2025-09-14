package com.gaviria.ormvsoptimizedsql.repo;

import com.gaviria.ormvsoptimizedsql.domain.CounterpartyAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CounterpartyAccountRepository extends JpaRepository<CounterpartyAccount, Long> {
}