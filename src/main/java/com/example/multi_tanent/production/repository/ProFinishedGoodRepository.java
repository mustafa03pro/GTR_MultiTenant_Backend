package com.example.multi_tanent.production.repository;

import com.example.multi_tanent.production.entity.ProFinishedGood;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProFinishedGoodRepository
        extends JpaRepository<ProFinishedGood, Long>, JpaSpecificationExecutor<ProFinishedGood> {
    List<ProFinishedGood> findByTenantId(Long tenantId);

    Optional<ProFinishedGood> findByTenantIdAndId(Long tenantId, Long id);

    boolean existsByTenantIdAndItemCodeIgnoreCase(Long tenantId, String itemCode);

    boolean existsByTenantIdAndNameIgnoreCase(Long tenantId, String name);
}
