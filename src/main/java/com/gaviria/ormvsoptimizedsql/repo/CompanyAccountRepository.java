package com.gaviria.ormvsoptimizedsql.repo;

import com.gaviria.ormvsoptimizedsql.domain.CompanyAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompanyAccountRepository extends JpaRepository<CompanyAccount, Long> {
    List<CompanyAccount> findCompanyAccountByCompany_Id(Long companyId);
}