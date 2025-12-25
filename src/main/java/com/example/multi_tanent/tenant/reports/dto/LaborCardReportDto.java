package com.example.multi_tanent.tenant.reports.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class LaborCardReportDto {
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String laborCardNumber;
    private LocalDate expiryDate;
    private String status; // ACTIVE, EXPIRED, EXPIRING_SOON
    private long daysToExpiry;
}
