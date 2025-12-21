package com.example.multi_tanent.pos.repository;

import com.example.multi_tanent.pos.entity.PaymentChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface PaymentChangeLogRepository extends JpaRepository<PaymentChangeLog, Long> {
    List<PaymentChangeLog> findByTenantIdAndCreatedAtBetween(Long tenantId, OffsetDateTime start, OffsetDateTime end);
    List<PaymentChangeLog> findByTenantIdAndStoreIdAndCreatedAtBetween(Long tenantId, Long storeId, OffsetDateTime start, OffsetDateTime end);
    List<PaymentChangeLog> findByTenantIdAndUserIdAndCreatedAtBetween(Long tenantId, Long userId, OffsetDateTime start, OffsetDateTime end);
}
