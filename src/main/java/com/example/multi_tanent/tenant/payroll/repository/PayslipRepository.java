package com.example.multi_tanent.tenant.payroll.repository;

import com.example.multi_tanent.tenant.payroll.entity.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PayslipRepository extends JpaRepository<Payslip, Long> {
    List<Payslip> findByEmployeeEmployeeCodeOrderByYearDescMonthDesc(String employeeCode);

    Optional<Payslip> findByEmployeeIdAndYearAndMonth(Long employeeId, int year, int month);

    List<Payslip> findByPayrollRunId(Long payrollRunId);

    // New method to fetch all details eagerly
    @Query("SELECT p FROM Payslip p JOIN FETCH p.employee e LEFT JOIN FETCH p.components pc LEFT JOIN FETCH pc.salaryComponent WHERE p.id = :id")
    Optional<Payslip> findByIdWithDetails(Long id);

    @Query("SELECT p FROM Payslip p JOIN FETCH p.employee e LEFT JOIN FETCH p.components pc LEFT JOIN FETCH pc.salaryComponent WHERE p.year = :year AND p.month = :month")
    List<Payslip> findAllByYearAndMonthWithDetails(@Param("year") int year, @Param("month") int month);
}