package com.example.multi_tanent.tenant.payroll.dto;

import com.example.multi_tanent.spersusers.enitity.CompanyBankAccount;

import lombok.Data;

@Data
public class CompanyBankAccountResponse {
    private Long id;
    private String bankName;
    private String accountNumber;
    private String ifscCode;
    private String accountHolderName;
    private String branchName;
    private boolean isPrimary;

    public static CompanyBankAccountResponse fromEntity(CompanyBankAccount account) {
        if (account == null)
            return null;
        CompanyBankAccountResponse dto = new CompanyBankAccountResponse();
        dto.setId(account.getId());
        dto.setBankName(account.getBankName());
        dto.setAccountNumber(account.getAccountNumber());
        dto.setIfscCode(account.getIfscCode());
        dto.setAccountHolderName(account.getAccountHolderName());
        dto.setBranchName(account.getBranchName());
        dto.setPrimary(account.isPrimary());
        return dto;
    }
}