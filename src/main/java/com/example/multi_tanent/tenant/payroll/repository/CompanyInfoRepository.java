package com.example.multi_tanent.tenant.payroll.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.multi_tanent.spersusers.enitity.CompanyInfo;

public interface CompanyInfoRepository extends JpaRepository<CompanyInfo, Long> {

    @Query("SELECT c FROM CompanyInfo c WHERE c.tenant.tenantId = :tenantId")
    java.util.Optional<CompanyInfo> findByTenantId(@Param("tenantId") String tenantId);
}