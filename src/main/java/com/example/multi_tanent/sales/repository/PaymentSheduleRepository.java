package com.example.multi_tanent.sales.repository;

import com.example.multi_tanent.sales.entity.PaymentShedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface PaymentSheduleRepository extends JpaRepository<PaymentShedule, Long> {
    Optional<PaymentShedule> findByIdAndTenantId(Long id, Long tenantId);

    Page<PaymentShedule> findByTenantId(Long tenantId, Pageable pageable);

    @Query("SELECT p FROM PaymentShedule p WHERE p.tenant.id = :tenantId AND " +
            "(:search IS NULL OR :search = '' OR lower(p.customer.companyName) LIKE lower(concat('%', :search, '%')) OR lower(p.rentalSalesOrder.orderNumber) LIKE lower(concat('%', :search, '%'))) AND "
            +
            "(:fromDate IS NULL OR p.dueDate >= :fromDate) AND " +
            "(:toDate IS NULL OR p.dueDate <= :toDate) AND " +
            "(:customerId IS NULL OR p.customer.id = :customerId) AND " +
            "(:rentalSalesOrderId IS NULL OR p.rentalSalesOrder.id = :rentalSalesOrderId)")
    Page<PaymentShedule> searchPaymentSchedules(
            @Param("tenantId") Long tenantId,
            @Param("search") String search,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("customerId") Long customerId,
            @Param("rentalSalesOrderId") Long rentalSalesOrderId,
            Pageable pageable);
}
