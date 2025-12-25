package com.example.multi_tanent.tenant.payroll.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class CompanyInfoRequest {
    private String logoUrl;
    private String companyName;
    private String address;
    private String city;
    private String emirate;
    private String poBox;
    private String country;
    private String phone;
    private String email;
    private String website;
    // UAE Statutory Details
    private String tradeLicenseNumber;
    private LocalDate tradeLicenseExpiry;
    private String trn; // Tax Registration Number
    private String mohreEstablishmentId;
    private String employerBankRoutingCode;
    private Integer visaQuotaTotal;
}
