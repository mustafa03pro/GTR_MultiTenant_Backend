package com.example.multi_tanent.tenant.reports.dto;

import com.example.multi_tanent.tenant.payroll.enums.TerminationReason;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class TerminationReasonReportDto {
    private TerminationReason reason;
    private long employeeCount;
    private List<EmployeeDetail> employees;

    @Data
    @Builder
    public static class EmployeeDetail {
        private String employeeCode;
        private String employeeName;
        private LocalDate lastWorkingDay;
    }
}
