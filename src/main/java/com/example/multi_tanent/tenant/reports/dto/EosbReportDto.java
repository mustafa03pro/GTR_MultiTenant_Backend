package com.example.multi_tanent.tenant.reports.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class EosbReportDto {
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private LocalDate joiningDate;
    private LocalDate lastWorkingDay;
    private BigDecimal totalYearsOfService;
    private BigDecimal lastBasicSalary;
    private BigDecimal gratuityAmount;
    private String calculationDetails;
}
