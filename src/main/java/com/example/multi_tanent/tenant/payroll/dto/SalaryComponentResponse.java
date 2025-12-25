package com.example.multi_tanent.tenant.payroll.dto;

import com.example.multi_tanent.tenant.payroll.entity.SalaryComponent;
import com.example.multi_tanent.tenant.payroll.enums.CalculationType;
import com.example.multi_tanent.tenant.payroll.enums.SalaryComponentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalaryComponentResponse {
    private Long id;
    private String code;
    private String name;
    private SalaryComponentType type;
    private CalculationType calculationType;
    private boolean isTaxable;
    private boolean isPartOfGrossSalary;
    private boolean isWpsIncluded;
    private boolean isVariable;

    public static SalaryComponentResponse fromEntity(SalaryComponent component) {
        return new SalaryComponentResponse(
                component.getId(), component.getCode(), component.getName(),
                component.getType(), component.getCalculationType(),
                Boolean.TRUE.equals(component.getIsTaxable()), Boolean.TRUE.equals(component.getIsPartOfGrossSalary()),
                Boolean.TRUE.equals(component.getIsWpsIncluded()), Boolean.TRUE.equals(component.getIsVariable()));
    }
}