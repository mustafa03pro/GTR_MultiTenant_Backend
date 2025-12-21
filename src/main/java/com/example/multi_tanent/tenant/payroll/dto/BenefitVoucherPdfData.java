package com.example.multi_tanent.tenant.payroll.dto;

import com.example.multi_tanent.spersusers.enitity.CompanyInfo;
import com.example.multi_tanent.tenant.payroll.entity.EmployeeBenefitProvision;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BenefitVoucherPdfData {
    private EmployeeBenefitProvision provision;
    private CompanyInfo companyInfo;

    public BenefitVoucherPdfData(EmployeeBenefitProvision provision, CompanyInfo companyInfo) {
        this.provision = provision;
        this.companyInfo = companyInfo;
    }
}