package com.example.multi_tanent.tenant.reports.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class FinalSettlementReportDto {
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private LocalDate lastWorkingDay;

    private BigDecimal gratuityAmount;
    private BigDecimal leaveEncashmentAmount;
    private BigDecimal totalDeductions;
    private BigDecimal netPayable;

    private String status; // PAID or PENDING
}
