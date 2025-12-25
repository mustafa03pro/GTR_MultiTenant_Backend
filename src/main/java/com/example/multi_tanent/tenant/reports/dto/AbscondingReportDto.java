package com.example.multi_tanent.tenant.reports.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class AbscondingReportDto {
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String department;
    private LocalDate reportedDate; // Assuming last updated date or similar for now
    private String status;
}
