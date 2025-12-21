package com.example.multi_tanent.tenant.payroll.service;

import com.example.multi_tanent.spersusers.enitity.CompanyBankAccount;
import com.example.multi_tanent.spersusers.enitity.CompanyInfo;
import com.example.multi_tanent.tenant.payroll.dto.CompanyBankAccountRequest;
import com.example.multi_tanent.tenant.payroll.repository.CompanyBankAccountRepository;
import com.example.multi_tanent.tenant.payroll.repository.CompanyInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(transactionManager = "tenantTx")
public class CompanyBankAccountService {

    private final CompanyBankAccountRepository bankAccountRepository;
    private final CompanyInfoRepository companyInfoRepository;

    public CompanyBankAccountService(CompanyBankAccountRepository bankAccountRepository,
            CompanyInfoRepository companyInfoRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.companyInfoRepository = companyInfoRepository;
    }

    public List<CompanyBankAccount> getAllBankAccounts() {
        return bankAccountRepository.findAll();
    }

    public CompanyBankAccount createBankAccount(CompanyBankAccountRequest request) {
        CompanyInfo companyInfo = companyInfoRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "CompanyInfo not found for this tenant. Please create it first."));

        CompanyBankAccount bankAccount = new CompanyBankAccount();
        mapRequestToEntity(request, bankAccount);
        bankAccount.setCompanyInfo(companyInfo);
        return bankAccountRepository.save(bankAccount);
    }

    public CompanyBankAccount updateBankAccount(Long id, CompanyBankAccountRequest request) {
        CompanyBankAccount bankAccount = bankAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("CompanyBankAccount not found with id: " + id));
        mapRequestToEntity(request, bankAccount);
        return bankAccountRepository.save(bankAccount);
    }

    public void deleteBankAccount(Long id) {
        bankAccountRepository.deleteById(id);
    }

    private void mapRequestToEntity(CompanyBankAccountRequest req, CompanyBankAccount entity) {
        entity.setBankName(req.getBankName());
        entity.setAccountNumber(req.getAccountNumber());
        entity.setIfscCode(req.getIfscCode());
        entity.setAccountHolderName(req.getAccountHolderName());
        entity.setBranchName(req.getBranchName());
        entity.setPrimary(req.isPrimary());
    }
}