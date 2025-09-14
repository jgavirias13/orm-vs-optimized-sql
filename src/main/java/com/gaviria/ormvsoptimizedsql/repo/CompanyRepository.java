package com.gaviria.ormvsoptimizedsql.repo;

import com.gaviria.ormvsoptimizedsql.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
}