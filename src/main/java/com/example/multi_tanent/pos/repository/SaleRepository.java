package com.example.multi_tanent.pos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.multi_tanent.pos.entity.Sale;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    List<Sale> findByTenantId(Long tenantId);

    Optional<Sale> findByIdAndTenantId(Long id, Long tenantId);

    List<Sale> findByTenantIdAndInvoiceDateBetween(Long tenantId, java.time.OffsetDateTime startDate,
            java.time.OffsetDateTime endDate);

    List<Sale> findByTenantIdAndStoreIdAndInvoiceDateBetween(Long tenantId, Long storeId,
            java.time.OffsetDateTime startDate,
            java.time.OffsetDateTime endDate);

    List<Sale> findByTenantIdAndStoreIdAndUserId(Long tenantId, Long storeId, Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM Sale s WHERE s.tenant.id = :tenantId " +
            "AND (:storeId IS NULL OR s.store.id = :storeId) " +
            "AND s.invoiceDate BETWEEN :startDate AND :endDate " +
            "AND (:source IS NULL OR s.salesSource = :source) " +
            "AND (:reference IS NULL OR s.salesSourceReference LIKE %:reference%)")
    List<Sale> findSalesBySourceFilters(Long tenantId, Long storeId, java.time.OffsetDateTime startDate,
            java.time.OffsetDateTime endDate, String source, String reference);
}
