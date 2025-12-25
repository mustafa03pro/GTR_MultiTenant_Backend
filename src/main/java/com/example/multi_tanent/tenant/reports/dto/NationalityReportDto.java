package com.example.multi_tanent.tenant.reports.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class NationalityReportDto {
    private String nationality;
    private long employeeCount;
    private double percentage;
    private List<EmployeeDetail> employees;

    @Data
    @Builder
    public static class EmployeeDetail {
        private String employeeCode;
        private String employeeName;
        private String designation;
    }
}
