package com.example.multi_tanent.sales.repository;

import com.example.multi_tanent.sales.entity.Quotation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuotationRepository extends JpaRepository<Quotation, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<Quotation> {
    Page<Quotation> findByTenantId(Long tenantId, Pageable pageable);

    Optional<Quotation> findByIdAndTenantId(Long id, Long tenantId);

    Optional<Quotation> findByQuotationNumberAndTenantId(String quotationNumber, Long tenantId);
}
