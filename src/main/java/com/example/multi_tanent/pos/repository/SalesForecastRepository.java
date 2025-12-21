package com.example.multi_tanent.pos.repository;

import com.example.multi_tanent.pos.entity.SalesForecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalesForecastRepository extends JpaRepository<SalesForecast, Long> {
    List<SalesForecast> findByTenantIdAndStoreIdAndMonthBetween(Long tenantId, Long storeId, LocalDate from,
            LocalDate to);

    Optional<SalesForecast> findByTenantIdAndStoreIdAndMonth(Long tenantId, Long storeId, LocalDate month);
}
