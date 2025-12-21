package com.example.multi_tanent.tenant.leave.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.multi_tanent.spersusers.enitity.Employee;

@Entity
@Table(name = "leave_allocations", uniqueConstraints = @UniqueConstraint(columnNames = { "employee_id", "leave_type_id",
        "period_start", "period_end" }))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // employee receiving allocation
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    // allocated days (can be fraction if you support half days)
    private BigDecimal allocatedDays;

    // period for which allocation applies (e.g., year)
    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    // reason or source (e.g., "Policy 2025", "Manual Adjustment")
    private String source;

    // audit
    private Long createdByUserId;
    private java.time.LocalDateTime createdAt;
}
