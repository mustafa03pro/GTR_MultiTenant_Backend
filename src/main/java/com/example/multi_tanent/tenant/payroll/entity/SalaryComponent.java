package com.example.multi_tanent.tenant.payroll.entity;

import com.example.multi_tanent.tenant.payroll.enums.CalculationType;
import com.example.multi_tanent.tenant.payroll.enums.SalaryComponentType;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "salary_components")
@Data
public class SalaryComponent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // e.g., "Basic", "HRA", "Provident Fund"

    @Column(nullable = false, unique = true)
    private String code; // e.g., "BASIC", "HRA", "PF"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SalaryComponentType type; // EARNING, DEDUCTION

    @Enumerated(EnumType.STRING)
    private CalculationType calculationType;

    private Boolean isTaxable;

    private Boolean isPartOfGrossSalary;

    private Integer displayOrder;

    @Column(name = "is_wps_included")
    private Boolean isWpsIncluded = true; // Default to true or false? Let's say false usually, but true for Basic.

    @Column(name = "is_variable")
    private Boolean isVariable = false; // Fixed by default
}