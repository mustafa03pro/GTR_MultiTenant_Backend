package com.example.multi_tanent.production.repository;

import com.example.multi_tanent.production.entity.ProcessFinishedGood;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProcessFinishedGoodRepository extends JpaRepository<ProcessFinishedGood, Long> {
    Page<ProcessFinishedGood> findByTenantId(Long tenantId, Pageable pageable);

    Optional<ProcessFinishedGood> findByIdAndTenantId(Long id, Long tenantId);

    Optional<ProcessFinishedGood> findByTenantIdAndId(Long tenantId, Long id);
}
