package com.example.multi_tanent.pos.repository;

import com.example.multi_tanent.pos.entity.CashRegister;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface CashRegisterRepository extends JpaRepository<CashRegister, Long> {
    List<CashRegister> findByTenantIdAndOpeningTimeBetween(Long tenantId, OffsetDateTime start, OffsetDateTime end);
}
