package com.example.multi_tanent.production.repository;

import com.example.multi_tanent.production.entity.ProductionSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface ProductionScheduleRepository extends JpaRepository<ProductionSchedule, Long> {

    @Query("SELECT ps FROM ProductionSchedule ps LEFT JOIN FETCH ps.manufacturingOrder LEFT JOIN FETCH ps.workGroup LEFT JOIN FETCH ps.employee "
            +
            "WHERE ps.tenant.tenantId = :tenantId AND " +
            "((ps.startTime BETWEEN :start AND :end) OR (ps.endTime BETWEEN :start AND :end) OR (ps.startTime <= :start AND ps.endTime >= :end))")
    List<ProductionSchedule> findAllByTenantIdAndDateRange(@Param("tenantId") String tenantId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end);

    @Query("SELECT ps FROM ProductionSchedule ps LEFT JOIN FETCH ps.manufacturingOrder LEFT JOIN FETCH ps.workGroup LEFT JOIN FETCH ps.employee "
            +
            "WHERE ps.tenant.tenantId = :tenantId AND ps.workGroup.id = :workGroupId AND " +
            "((ps.startTime BETWEEN :start AND :end) OR (ps.endTime BETWEEN :start AND :end) OR (ps.startTime <= :start AND ps.endTime >= :end))")
    List<ProductionSchedule> findByTenantIdAndWorkGroupIdAndDateRange(@Param("tenantId") String tenantId,
            @Param("workGroupId") Long workGroupId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end);

    @Query("SELECT ps FROM ProductionSchedule ps LEFT JOIN FETCH ps.manufacturingOrder LEFT JOIN FETCH ps.workGroup LEFT JOIN FETCH ps.employee "
            +
            "WHERE ps.tenant.tenantId = :tenantId AND ps.employee.id = :employeeId AND " +
            "((ps.startTime BETWEEN :start AND :end) OR (ps.endTime BETWEEN :start AND :end) OR (ps.startTime <= :start AND ps.endTime >= :end))")
    List<ProductionSchedule> findByTenantIdAndEmployeeIdAndDateRange(@Param("tenantId") String tenantId,
            @Param("employeeId") Long employeeId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end);
}
