package com.example.multi_tanent.production.repository;

import com.example.multi_tanent.production.entity.ProProcess;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProProcessRepository extends JpaRepository<ProProcess, Long> {
    Page<ProProcess> findByTenantId(Long tenantId, Pageable pageable);

    Optional<ProProcess> findByIdAndTenantId(Long id, Long tenantId);

    Optional<ProProcess> findByTenantIdAndId(Long tenantId, Long id);
}