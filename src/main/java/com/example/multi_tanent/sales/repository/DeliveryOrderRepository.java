package com.example.multi_tanent.sales.repository;

import com.example.multi_tanent.sales.entity.DeliveryOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeliveryOrderRepository
        extends JpaRepository<DeliveryOrder, Long>, JpaSpecificationExecutor<DeliveryOrder> {
    Page<DeliveryOrder> findByTenantId(Long tenantId, Pageable pageable);

    Optional<DeliveryOrder> findByIdAndTenantId(Long id, Long tenantId);
}
