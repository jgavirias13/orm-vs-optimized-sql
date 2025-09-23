package com.gaviria.ormvsoptimizedsql.repo;

import com.gaviria.ormvsoptimizedsql.domain.Movement;
import com.gaviria.ormvsoptimizedsql.repo.projection.AggMovementDay;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

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

    @Query("""
              select new com.gaviria.ormvsoptimizedsql.repo.projection.AggMovementDay(
                  m.companyAccount.id,
                  m.currency.code,
                  function('date', m.bookedAt),
                  sum(m.amount)
              )
              from Movement m
              where m.companyAccount.company.id = :companyId
                and m.bookedAt >= :from and m.bookedAt < :to
              group by m.companyAccount.id, m.currency.code, function('date', m.bookedAt)
            """)
    List<AggMovementDay> aggregateByAccountCurrencyDay(
            @Param("companyId") Long companyId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

}