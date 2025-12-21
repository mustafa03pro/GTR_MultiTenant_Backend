package com.example.multi_tanent.sales.repository;

import com.example.multi_tanent.sales.entity.RentalQuotation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RentalQuotationRepository extends JpaRepository<RentalQuotation, Long> {
        Page<RentalQuotation> findByTenantId(Long tenantId, Pageable pageable);

        @org.springframework.data.jpa.repository.Query("SELECT rq FROM RentalQuotation rq WHERE rq.tenant.id = :tenantId "
                        +
                        "AND (:customerName IS NULL OR LOWER(rq.customer.companyName) LIKE LOWER(CONCAT('%', :customerName, '%'))) "
                        +
                        "AND (:fromDate IS NULL OR rq.quotationDate >= :fromDate) " +
                        "AND (:toDate IS NULL OR rq.quotationDate <= :toDate) " +
                        "AND (:status IS NULL OR rq.status = :status) " +
                        "AND (:salespersonId IS NULL OR rq.salesperson.id = :salespersonId) " +
                        "AND (:quotationType IS NULL OR rq.quotationType = :quotationType)")
        Page<RentalQuotation> searchRentalQuotations(Long tenantId, String customerName, java.time.LocalDate fromDate,
                        java.time.LocalDate toDate, com.example.multi_tanent.sales.enums.SalesStatus status,
                        Long salespersonId, com.example.multi_tanent.sales.enums.QuotationType quotationType,
                        Pageable pageable);

        Optional<RentalQuotation> findByIdAndTenantId(Long id, Long tenantId);

        Optional<RentalQuotation> findByQuotationNumberAndTenantId(String quotationNumber, Long tenantId);
}
