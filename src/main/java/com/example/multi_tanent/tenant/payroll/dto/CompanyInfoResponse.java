package com.example.multi_tanent.tenant.payroll.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com.example.multi_tanent.spersusers.enitity.CompanyInfo;

@Data
@Builder
public class CompanyInfoResponse {
    private Long id;
    private String companyName;
    private String logoUrl;
    private String address;
    private String city;
    private String emirate;
    private String poBox;
    private String country;
    private String phone;
    private String email;
    private String website;
    private String tradeLicenseNumber;
    private LocalDate tradeLicenseExpiry;
    private String trn;
    private String mohreEstablishmentId;
    private String employerBankRoutingCode;
    private List<CompanyLocationResponse> locations;
    private List<CompanyBankAccountResponse> bankAccounts;

    public static CompanyInfoResponse fromEntity(CompanyInfo entity) {
        if (entity == null)
            return null;
        CompanyInfoResponseBuilder builder = CompanyInfoResponse.builder()
                .id(entity.getId())
                .companyName(entity.getCompanyName())
                .logoUrl(entity.getLogoUrl())
                .address(entity.getAddress())
                .city(entity.getCity())
                .emirate(entity.getEmirate())
                .poBox(entity.getPoBox())
                .country(entity.getCountry())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .website(entity.getWebsite())
                .tradeLicenseNumber(entity.getTradeLicenseNumber())
                .tradeLicenseExpiry(entity.getTradeLicenseExpiry())
                .trn(entity.getTrn())
                .mohreEstablishmentId(entity.getMohreEstablishmentId())
                .employerBankRoutingCode(entity.getEmployerBankRoutingCode());

        if (entity.getLocations() != null) {
            builder.locations(entity.getLocations().stream()
                    .map(CompanyLocationResponse::fromEntity)
                    .collect(Collectors.toList()));
        }

        if (entity.getBankAccounts() != null) {
            builder.bankAccounts(entity.getBankAccounts().stream()
                    .map(CompanyBankAccountResponse::fromEntity)
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }
}