package com.example.multi_tanent.tenant.payroll.entity;

import com.example.multi_tanent.spersusers.enitity.Employee;
import com.example.multi_tanent.tenant.payroll.enums.PayrollStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "payslips", indexes = {
        @Index(name = "idx_payslip_employee_period", columnList = "employee_id, year, month")
})
@Data
public class Payslip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_run_id")
    private PayrollRun payrollRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int month;

    @Column(nullable = false)
    private LocalDate payDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal grossEarnings;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalDeductions;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal netSalary;

    @Column(name = "work_expenses", precision = 15, scale = 2)
    private BigDecimal workExpenses = BigDecimal.ZERO;

    @Column(name = "net_additions", precision = 15, scale = 2)
    private BigDecimal netAdditions = BigDecimal.ZERO;

    @Column(name = "arrears_addition", precision = 15, scale = 2)
    private BigDecimal arrearsAddition = BigDecimal.ZERO;

    @Column(name = "arrears_deduction", precision = 15, scale = 2)
    private BigDecimal arrearsDeduction = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayrollStatus status;

    @OneToMany(mappedBy = "payslip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PayslipComponent> components;

    // Fields for additional payslip details
    private Integer totalDaysInMonth;
    private BigDecimal payableDays;
    private BigDecimal lossOfPayDays;

    @Column(length = 1000)
    private String leaveBalanceSummary; // e.g., "CL: 5.0, SL: 10.0, EL: 12.5"

}