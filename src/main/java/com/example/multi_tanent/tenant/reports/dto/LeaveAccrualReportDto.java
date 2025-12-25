package com.example.multi_tanent.tenant.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveAccrualReportDto {
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String department;
    private String designation;
    private LocalDate joiningDate;

    private String leaveType;

    // As per 30 days/year calculation
    private BigDecimal accruedDays;

    // Actually taken (Approved)
    private BigDecimal takenDays;

    private BigDecimal balanceDays;
}
