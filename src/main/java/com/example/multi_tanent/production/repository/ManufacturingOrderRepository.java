package com.example.multi_tanent.production.repository;

import com.example.multi_tanent.production.entity.ManufacturingOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ManufacturingOrderRepository
        extends JpaRepository<ManufacturingOrder, Long>, JpaSpecificationExecutor<ManufacturingOrder> {
    Page<ManufacturingOrder> findByTenantId(Long tenantId, Pageable pageable);

    Optional<ManufacturingOrder> findByTenantIdAndId(Long tenantId, Long id);

    boolean existsByTenantIdAndMoNumber(Long tenantId, String moNumber);
}
