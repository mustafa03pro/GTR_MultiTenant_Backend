package com.example.multi_tanent.tenant.reports.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmiratizationReportDto {
    private long totalEmployees;
    private long totalNationals; // Emiratis
    private long totalExpats;
    private double emiratizationPercentage;
    private String status; // COMPLIANT / NON_COMPLIANT (e.g., target 2%)
}
