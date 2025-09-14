package com.gaviria.ormvsoptimizedsql.repo;

import com.gaviria.ormvsoptimizedsql.domain.CompanyAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyAccountRepository extends JpaRepository<CompanyAccount, Long> {
}