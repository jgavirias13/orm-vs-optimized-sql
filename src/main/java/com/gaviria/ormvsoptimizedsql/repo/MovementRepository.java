package com.gaviria.ormvsoptimizedsql.repo;

import com.gaviria.ormvsoptimizedsql.domain.Movement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface MovementRepository extends JpaRepository<Movement, Long> {

    @Query("""
              select m from Movement m
              where m.companyAccount.id = :companyAccountId
                and m.bookedAt between :from and :to
              order by m.bookedAt asc
            """)
    Page<Movement> findByCompanyAccountAndPeriod(
            @Param("companyAccountId") Long companyAccountId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

}