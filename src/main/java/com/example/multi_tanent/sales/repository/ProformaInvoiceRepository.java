package com.example.multi_tanent.sales.repository;

import com.example.multi_tanent.sales.entity.ProformaInvoice;
import com.example.multi_tanent.spersusers.enitity.Tenant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface ProformaInvoiceRepository extends JpaRepository<ProformaInvoice, Long> {

    Optional<ProformaInvoice> findByIdAndTenantId(Long id, Long tenantId);

    Page<ProformaInvoice> findByTenantId(Long tenantId, Pageable pageable);

    @Query("SELECT p FROM ProformaInvoice p WHERE p.tenant.id = :tenantId AND " +
            "(:search IS NULL OR :search = '' OR lower(p.invoiceNumber) LIKE lower(concat('%', :search, '%')) OR lower(p.reference) LIKE lower(concat('%', :search, '%')) OR lower(p.customer.companyName) LIKE lower(concat('%', :search, '%'))) AND "
            +
            "(:fromDate IS NULL OR p.invoiceDate >= :fromDate) AND " +
            "(:toDate IS NULL OR p.invoiceDate <= :toDate) AND " +
            "(:salespersonId IS NULL OR p.salesperson.id = :salespersonId)")
    Page<ProformaInvoice> searchProformaInvoices(@Param("tenantId") Long tenantId,
            @Param("search") String search,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("salespersonId") Long salespersonId,
            Pageable pageable);

    Optional<Tenant> findByInvoiceNumberAndTenantId(String invoiceNumber, Long id);
}
