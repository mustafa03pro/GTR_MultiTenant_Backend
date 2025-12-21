package com.example.multi_tanent.tenant.payroll.dto;

import com.example.multi_tanent.spersusers.enitity.CompanyInfo;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.tenant.employee.entity.JobDetails;
import com.example.multi_tanent.tenant.payroll.entity.EndOfService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinalSettlementPdfData {
    private EndOfService endOfService;
    private CompanyInfo companyInfo;
    private Tenant tenant;
    private JobDetails jobDetails;
    // You can add more fields here if needed, like Leave Encashment details
}