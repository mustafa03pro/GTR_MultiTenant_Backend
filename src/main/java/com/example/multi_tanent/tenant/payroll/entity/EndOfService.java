package com.example.multi_tanent.tenant.payroll.entity;

import com.example.multi_tanent.spersusers.enitity.Employee;
import com.example.multi_tanent.tenant.payroll.enums.TerminationReason;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "end_of_service")
@Data
public class EndOfService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false, unique = true)
    private Employee employee;

    @Column(nullable = false)
    private LocalDate joiningDate;

    @Column(nullable = false)
    private LocalDate lastWorkingDay;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal totalYearsOfService;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal lastBasicSalary;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal gratuityAmount;

    @Column(length = 1000)
    private String calculationDetails; // Store a summary of the calculation logic used

    @Enumerated(EnumType.STRING)
    private TerminationReason terminationReason;

    private boolean isPaid = false;

    private LocalDate paymentDate;

    private LocalDateTime calculatedAt;

}