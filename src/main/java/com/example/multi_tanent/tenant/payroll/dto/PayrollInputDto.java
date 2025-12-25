package com.example.multi_tanent.tenant.payroll.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class PayrollInputDto {
    private String employeeCode;

    // Key: Salary Component Code (e.g., "OVERTIME", "COMMISSION"), Value: Amount
    private Map<String, BigDecimal> variableComponents;

    // Optional: Days worked if different from standard
    private BigDecimal payableDays;

    // Optional: Loss of pay days
    private BigDecimal lossOfPayDays;

    // Explicit columns from Excel
    private BigDecimal workExpenses;
    private BigDecimal netAdditions;
    private BigDecimal arrearsAddition;
    private BigDecimal arrearsDeduction;
}
