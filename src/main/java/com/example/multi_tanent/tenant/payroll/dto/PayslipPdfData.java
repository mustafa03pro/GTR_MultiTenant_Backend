package com.example.multi_tanent.tenant.payroll.dto;

import com.example.multi_tanent.spersusers.enitity.CompanyInfo;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.tenant.employee.entity.EmployeeProfile;
import com.example.multi_tanent.tenant.employee.entity.JobDetails;
import com.example.multi_tanent.tenant.payroll.entity.Payslip;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayslipPdfData {
    private Payslip payslip;
    private CompanyInfo companyInfo;
    private Tenant tenant;
    private JobDetails jobDetails;
    private EmployeeProfile employeeProfile;

    public String getEmployeeFullName() {
        if (payslip != null && payslip.getEmployee() != null) {
            return payslip.getEmployee().getFirstName() + " " + payslip.getEmployee().getLastName();
        }
        return "N/A";
    }
}