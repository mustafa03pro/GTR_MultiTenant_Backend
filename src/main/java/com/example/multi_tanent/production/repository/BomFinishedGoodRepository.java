package com.example.multi_tanent.production.repository;

import com.example.multi_tanent.production.entity.BomFinishedGood;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BomFinishedGoodRepository extends JpaRepository<BomFinishedGood, Long> {

    Optional<BomFinishedGood> findByTenantIdAndId(Long tenantId, Long id);

    Page<BomFinishedGood> findByTenantId(Long tenantId, Pageable pageable);

    List<BomFinishedGood> findByTenantId(Long tenantId);

    boolean existsByTenantIdAndBomNameIgnoreCase(Long tenantId, String bomName);
}
