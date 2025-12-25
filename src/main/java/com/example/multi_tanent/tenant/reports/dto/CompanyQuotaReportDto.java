package com.example.multi_tanent.tenant.reports.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompanyQuotaReportDto {
    private String companyName;
    private Integer totalVisaQuota;
    private Integer usedVisaQuota;
    private Integer availableVisaQuota;
    private Double utilizationPercentage;
}
