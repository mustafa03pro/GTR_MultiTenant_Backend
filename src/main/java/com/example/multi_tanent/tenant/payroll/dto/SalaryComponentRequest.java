package com.example.multi_tanent.tenant.payroll.dto;

import com.example.multi_tanent.tenant.payroll.enums.CalculationType;
import com.example.multi_tanent.tenant.payroll.enums.SalaryComponentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SalaryComponentRequest {
    @NotBlank(message = "Component code cannot be blank.")
    @Size(max = 20, message = "Component code cannot exceed 20 characters.")
    private String code;

    @NotBlank(message = "Component name cannot be blank.")
    @Size(max = 100, message = "Component name cannot exceed 100 characters.")
    private String name;

    @NotNull(message = "Component type must be specified.")
    private SalaryComponentType type;

    @NotNull(message = "Calculation type must be specified.")
    private CalculationType calculationType;

    @NotNull(message = "isTaxable flag must be provided.")
    private Boolean isTaxable = true;

    @NotNull(message = "isPartOfGrossSalary flag must be provided.")
    private Boolean isPartOfGrossSalary = true;

    private Boolean isWpsIncluded = true; // Optional, default true if null? Service can handle default.
    private Boolean isVariable = false;
}