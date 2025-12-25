package com.example.multi_tanent.tenant.payroll.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class WpsComplianceReportDto {
    private int totalEmployees;
    private int paidEmployees;
    private int unpaidEmployees;
    private BigDecimal compliantPercentage;
    private List<EmployeeComplianceDetail> details;

    @Data
    @Builder
    public static class EmployeeComplianceDetail {
        private Long employeeId;
        private String employeeCode;
        private String employeeName;
        private String status; // PAID, UNPAID
        private LocalDate payDate;
        private Long delayDays; // Days delayed beyond cut-off or relative to period end
        private String remarks; // e.g. "On Time", "Delayed by X days"
    }
}
