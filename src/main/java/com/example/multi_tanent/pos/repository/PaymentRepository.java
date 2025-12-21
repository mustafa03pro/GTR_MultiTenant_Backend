package com.example.multi_tanent.pos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.multi_tanent.pos.entity.Payment;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByIdAndSaleId(Long paymentId, Long saleId);

    java.util.List<Payment> findBySaleTenantIdAndCreatedAtBetween(Long tenantId, java.time.OffsetDateTime startDate,
            java.time.OffsetDateTime endDate);

    java.util.List<Payment> findBySaleTenantIdAndSaleStoreIdAndCreatedAtBetween(Long tenantId, Long storeId,
            java.time.OffsetDateTime startDate, java.time.OffsetDateTime endDate);
}
