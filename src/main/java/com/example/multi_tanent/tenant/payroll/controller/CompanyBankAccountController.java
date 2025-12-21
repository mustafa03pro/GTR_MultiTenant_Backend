package com.example.multi_tanent.tenant.payroll.controller;

import com.example.multi_tanent.spersusers.enitity.CompanyBankAccount;
import com.example.multi_tanent.tenant.payroll.dto.CompanyBankAccountRequest;
import com.example.multi_tanent.tenant.payroll.dto.CompanyBankAccountResponse;
import com.example.multi_tanent.tenant.payroll.service.CompanyBankAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/company-bank-accounts")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','HRMS_ADMIN','HR','MANAGER')")
public class CompanyBankAccountController {

    private final CompanyBankAccountService bankAccountService;

    public CompanyBankAccountController(CompanyBankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @GetMapping
    public ResponseEntity<List<CompanyBankAccountResponse>> getAllBankAccounts() {
        List<CompanyBankAccountResponse> accounts = bankAccountService.getAllBankAccounts().stream()
                .map(CompanyBankAccountResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(accounts);
    }

    @PostMapping
    public ResponseEntity<CompanyBankAccountResponse> createBankAccount(
            @RequestBody CompanyBankAccountRequest request) {
        CompanyBankAccount createdAccount = bankAccountService.createBankAccount(request);
        URI locationUri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(createdAccount.getId()).toUri();
        return ResponseEntity.created(locationUri).body(CompanyBankAccountResponse.fromEntity(createdAccount));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompanyBankAccountResponse> updateBankAccount(@PathVariable Long id,
            @RequestBody CompanyBankAccountRequest request) {
        return ResponseEntity
                .ok(CompanyBankAccountResponse.fromEntity(bankAccountService.updateBankAccount(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBankAccount(@PathVariable Long id) {
        bankAccountService.deleteBankAccount(id);
        return ResponseEntity.noContent().build();
    }
}
