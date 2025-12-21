package com.example.multi_tanent.sales.repository;

import com.example.multi_tanent.sales.entity.SalesOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<SalesOrder> {
    Page<SalesOrder> findByTenantId(Long tenantId, Pageable pageable);

    Optional<SalesOrder> findByIdAndTenantId(Long id, Long tenantId);

    Optional<SalesOrder> findBySalesOrderNumberAndTenantId(String salesOrderNumber, Long tenantId);
}
