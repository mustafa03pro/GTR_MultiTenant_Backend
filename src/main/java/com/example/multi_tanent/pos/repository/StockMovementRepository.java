package com.example.multi_tanent.pos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.multi_tanent.pos.entity.StockMovement;

import java.util.List;
import java.util.Optional;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByTenantId(Long tenantId);

    Optional<StockMovement> findByIdAndTenantId(Long id, Long tenantId);

    /**
     * Deletes all stock movement records associated with a given list of product
     * variant IDs.
     * This is used to clean up history before deleting a product.
     * 
     * @param productVariantIds A list of product variant IDs.
     */
    void deleteByProductVariantIdIn(List<Long> productVariantIds);

    List<StockMovement> findByTenantIdAndReasonContainingIgnoreCaseAndCreatedAtBetween(
            Long tenantId, String reason, java.time.OffsetDateTime startDate, java.time.OffsetDateTime endDate);

    @org.springframework.data.jpa.repository.Query("SELECT sm FROM StockMovement sm " +
            "JOIN sm.productVariant pv " +
            "JOIN pv.product p " +
            "WHERE sm.tenant.id = :tenantId " +
            "AND sm.createdAt BETWEEN :startDate AND :endDate " +
            "AND (:storeId IS NULL OR sm.store.id = :storeId) " +
            "AND (:itemName IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :itemName, '%')))")
    List<StockMovement> findItemMovements(
            @org.springframework.web.bind.annotation.RequestParam("tenantId") Long tenantId,
            @org.springframework.web.bind.annotation.RequestParam("startDate") java.time.OffsetDateTime startDate,
            @org.springframework.web.bind.annotation.RequestParam("endDate") java.time.OffsetDateTime endDate,
            @org.springframework.web.bind.annotation.RequestParam("storeId") Long storeId,
            @org.springframework.web.bind.annotation.RequestParam("itemName") String itemName);
}
