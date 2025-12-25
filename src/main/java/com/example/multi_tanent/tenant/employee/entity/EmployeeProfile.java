package com.example.multi_tanent.tenant.employee.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import com.example.multi_tanent.spersusers.enitity.Employee;
import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "employee_profile")
@Getter
@Setter
@ToString(exclude = "employee")
@EqualsAndHashCode(exclude = "employee")
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    @JsonBackReference
    private Employee employee;

    @Column(name = "job_title")
    private String jobTitle;

    private String department;

    @Column(name = "preferred_name")
    private String preferredName;

    @Column(name = "job_type")
    private String jobType;

    private String office;

    @Column(name = "mol_id")
    private String molId;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "hire_date")
    private java.time.LocalDate hireDate;

    @Column(name = "is_wps_registered")
    private boolean isWpsRegistered = true; // Default to true as per requirements usually

    @Column(name = "labor_card_number")
    private String laborCardNumber; // MOHRE Person ID

    @Column(name = "labor_card_expiry")
    private java.time.LocalDate laborCardExpiry;

    private String nationality;

    @Column(name = "routing_code")
    private String routingCode; // Agent ID (Bank routing code)

    @Column(length = 1000)
    private String address;

    private String city;
    private String state;
    private String country;

    @Column(name = "postal_code", length = 30)
    private String postalCode;

    private String emergencyContactName;
    private String emergencyContactRelation;
    private String emergencyContactPhone;
    private String bankName;
    private String bankAccountNumber;
    private String iban;
    private String ifscCode;
    private String bloodGroup;

    @Column(length = 2000)
    private String notes;

}
