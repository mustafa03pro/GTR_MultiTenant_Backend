package com.example.multi_tanent.tenant.payroll.entity;

import com.example.multi_tanent.spersusers.enitity.Employee;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "employee_bank_accounts")
@Data
public class EmployeeBankAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false, unique = true)
    private Employee employee;

    @Column(nullable = false)
    private String bankName;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String ifscCode;

    @Column(length = 34)
    private String iban;

    @Column(name = "routing_code")
    private String routingCode; // Agent ID (Bank routing code for WPS)

    private String accountHolderName;

    private boolean isPrimary;
}