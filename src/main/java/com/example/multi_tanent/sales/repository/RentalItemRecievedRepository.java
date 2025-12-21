package com.example.multi_tanent.sales.repository;

import com.example.multi_tanent.sales.entity.RentalItemRecieved;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RentalItemRecievedRepository extends JpaRepository<RentalItemRecieved, Long> {
    List<RentalItemRecieved> findByTenant_Id(Long tenantId);

    List<RentalItemRecieved> findByRentalSalesOrder_Id(Long rentalSalesOrderId);

    java.util.Optional<RentalItemRecieved> findByIdAndTenantId(Long id, Long tenantId);
}
