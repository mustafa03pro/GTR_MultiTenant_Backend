package com.example.multi_tanent.spersusers.enitity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "company_info")
@Getter
@Setter
@ToString(exclude = { "locations", "bankAccounts", "tenant" })
public class CompanyInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Should only be one record per tenant

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "id")
    @org.hibernate.annotations.NotFound(action = org.hibernate.annotations.NotFoundAction.IGNORE)
    private Tenant tenant;

    @Column(nullable = false)
    private String companyName;

    private String logoUrl; // URL or path to the company logo

    // These fields can represent the primary/registered office address.
    // Additional branch offices can be managed via the 'locations' list.
    private String address;
    private String city;
    private String emirate; // More specific to UAE context than 'state'
    private String poBox;
    private String country;
    private String phone;
    private String email;
    private String website;

    // --- UAE-Specific Statutory Details ---

    @Column(name = "trade_license_number")
    private String tradeLicenseNumber;

    @Column(name = "trade_license_expiry")
    private LocalDate tradeLicenseExpiry;

    @Column(name = "trn")
    private String trn; // Tax Registration Number for VAT

    @Column(name = "mohre_establishment_id")
    private String mohreEstablishmentId; // Ministry of Human Resources & Emiratisation ID

    @OneToMany(mappedBy = "companyInfo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompanyLocation> locations;

    @OneToMany(mappedBy = "companyInfo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompanyBankAccount> bankAccounts;
}