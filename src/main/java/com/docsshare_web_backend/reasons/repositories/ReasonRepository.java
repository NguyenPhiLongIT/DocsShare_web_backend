package com.docsshare_web_backend.reasons.repositories;

import com.docsshare_web_backend.reasons.models.Reason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ReasonRepository extends JpaRepository<Reason, Long>, JpaSpecificationExecutor<Reason> {
    Optional<Reason> findByName(String name);
    boolean existsByName(String name);
}
