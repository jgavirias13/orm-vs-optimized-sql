package com.gaviria.ormvsoptimizedsql.repo;

import com.gaviria.ormvsoptimizedsql.domain.Movement;
import com.gaviria.ormvsoptimizedsql.repo.projection.AggCounterpartyDayView;
import com.gaviria.ormvsoptimizedsql.repo.projection.AggMovementDayView;
import com.gaviria.ormvsoptimizedsql.repo.projection.MovementLineView;
import com.gaviria.ormvsoptimizedsql.repo.projection.MovementTagPairView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
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
              select
                m.companyAccount.id          as accountId,
                m.currency.code              as currency,
                function('date', m.bookedAt) as day,
                sum(m.amount)                as totalAmount
              from Movement m
              where m.companyAccount.company.id = :companyId
                and m.bookedAt >= :from and m.bookedAt < :to
              group by m.companyAccount.id, m.currency.code, function('date', m.bookedAt)
            """)
    List<AggMovementDayView> aggregateByAccountCurrencyDay(
            @Param("companyId") Long companyId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
              select
                m.counterpartyAccount.counterparty.displayName as counterparty,
                m.currency.code                                 as currency,
                function('date', m.bookedAt)                    as day,
                sum(m.amount)                                   as totalAmount,
                count(m.id)                                     as txCount
              from Movement m
              where m.companyAccount.company.id = :companyId
                and m.bookedAt >= :from and m.bookedAt < :to
              group by m.counterpartyAccount.counterparty.displayName,
                       m.currency.code,
                       function('date', m.bookedAt)
            """)
    List<AggCounterpartyDayView> aggregateByCounterpartyCurrencyDay(
            @Param("companyId") Long companyId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
              select
                m.id                           as id,
                m.bookedAt                     as bookedAt,
                m.counterpartyAccount.counterparty.displayName as counterparty,
                m.currency.code                as currency,
                m.amount                       as amount,
                m.description                  as description
              from Movement m
              where m.companyAccount.id = :companyAccountId
                and m.bookedAt >= :from and m.bookedAt < :to
              order by m.bookedAt asc, m.id asc
            """)
    Page<MovementLineView> pageLinesForAccountAndPeriod(
            @Param("companyAccountId") Long companyAccountId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    @Query("""
              select m.id as movementId, t.name as tagName
              from Movement m
                join m.tags t
              where m.id in :movementIds
            """)
    List<MovementTagPairView> findTagsForMovements(@Param("movementIds") Collection<Long> movementIds);

}