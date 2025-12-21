package com.example.multi_tanent.sales.repository;

import com.example.multi_tanent.sales.entity.FollowUp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {
    List<FollowUp> findByTenant_Id(Long tenantId);

    Optional<FollowUp> findByIdAndTenantId(Long id, Long tenantId);

    List<FollowUp> findByQuotation_Id(Long quotationId);

    List<FollowUp> findByRentalQuotation_Id(Long rentalQuotationId);
}
