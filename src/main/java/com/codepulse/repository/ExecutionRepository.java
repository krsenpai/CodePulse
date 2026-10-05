package com.codepulse.repository;

import com.codepulse.entity.Execution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExecutionRepository extends JpaRepository<Execution, Long> {

    Page<Execution> findByUserUsername(
            String username,
            Pageable pageable
    );

    Optional<Execution> findByIdAndUserUsername(
            Long id,
            String username
    );
}