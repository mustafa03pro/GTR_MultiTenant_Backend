package com.example.multi_tanent.spersusers.enitity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "company_bank_accounts")
@Data
public class CompanyBankAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_info_id", nullable = false)
    @JsonIgnore // Avoid serialization loops
    @ToString.Exclude // Avoid recursion in toString()
    @EqualsAndHashCode.Exclude // Avoid recursion in equals/hashCode
    private CompanyInfo companyInfo;

    @Column(nullable = false)
    private String bankName;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String ifscCode;

    private String accountHolderName;
    private String branchName;

    private boolean isPrimary; // For the primary salary disbursement account
}