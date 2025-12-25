package com.example.multi_tanent.tenant.attendance.repository;

import com.example.multi_tanent.tenant.attendance.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    Optional<AttendanceRecord> findByEmployeeEmployeeCodeAndAttendanceDate(String employeeCode,
            LocalDate attendanceDate);

    @Query("SELECT ar FROM AttendanceRecord ar LEFT JOIN FETCH ar.employee LEFT JOIN FETCH ar.attendancePolicy WHERE ar.employee.employeeCode = :employeeCode AND ar.attendanceDate BETWEEN :startDate AND :endDate")
    List<AttendanceRecord> findByEmployeeEmployeeCodeAndAttendanceDateBetweenWithDetails(
            @Param("employeeCode") String employeeCode, @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT ar FROM AttendanceRecord ar LEFT JOIN FETCH ar.employee LEFT JOIN FETCH ar.attendancePolicy")
    List<AttendanceRecord> findAllWithDetails();

    @Query("SELECT ar FROM AttendanceRecord ar LEFT JOIN FETCH ar.employee e LEFT JOIN FETCH ar.attendancePolicy WHERE e.user.tenant.tenantId = :tenantId AND ar.attendanceDate = :date")
    List<AttendanceRecord> findAllByTenantIdAndDateWithDetails(@Param("tenantId") String tenantId,
            @Param("date") LocalDate date);

    @Query("SELECT ar FROM AttendanceRecord ar LEFT JOIN FETCH ar.employee e LEFT JOIN FETCH ar.attendancePolicy WHERE e.user.tenant.tenantId = :tenantId AND ar.attendanceDate BETWEEN :startDate AND :endDate")
    List<AttendanceRecord> findAllByTenantIdAndDateBetweenWithDetails(@Param("tenantId") String tenantId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}