package com.example.multi_tanent.tenant.employee.dto;

import lombok.Data;

@Data
public class EmployeeProfileRequest {
    private String address;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String emergencyContactName;
    private String emergencyContactRelation;
    private String emergencyContactPhone;
    private String laborCardNumber; // MOHRE Person ID
    private String routingCode; // Agent ID (Bank routing code)
    private String bankName;
    private String bankAccountNumber;
    private String ifscCode;
    private String jobTitle;
    private String department;
    private java.time.LocalDate hireDate;
    private boolean isWpsRegistered;
    private String iban;
    private String bloodGroup;
    private String notes;
    private String preferredName;
    private String jobType;
    private String office;
    private String molId;
    private String paymentMethod;
    private java.time.LocalDate laborCardExpiry;
    private String nationality;
}