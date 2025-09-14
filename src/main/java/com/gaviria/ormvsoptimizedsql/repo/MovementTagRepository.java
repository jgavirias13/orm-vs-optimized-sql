package com.gaviria.ormvsoptimizedsql.repo;

import com.gaviria.ormvsoptimizedsql.domain.MovementTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovementTagRepository extends JpaRepository<MovementTag, Long> {
}