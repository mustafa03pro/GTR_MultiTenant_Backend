package com.example.multi_tanent.sales.repository;

import com.example.multi_tanent.sales.entity.RentalInvoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RentalInvoiceRepository
        extends JpaRepository<RentalInvoice, Long>, JpaSpecificationExecutor<RentalInvoice> {
    Page<RentalInvoice> findByTenantId(Long tenantId, Pageable pageable);

    Optional<RentalInvoice> findByIdAndTenantId(Long id, Long tenantId);
}
