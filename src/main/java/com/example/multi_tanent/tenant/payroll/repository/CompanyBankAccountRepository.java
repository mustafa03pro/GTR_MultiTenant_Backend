package com.example.multi_tanent.tenant.payroll.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.multi_tanent.spersusers.enitity.CompanyBankAccount;

public interface CompanyBankAccountRepository extends JpaRepository<CompanyBankAccount, Long> {
}