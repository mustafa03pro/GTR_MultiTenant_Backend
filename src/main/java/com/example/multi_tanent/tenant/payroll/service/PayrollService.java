package com.example.multi_tanent.tenant.payroll.service;

import com.example.multi_tanent.spersusers.enitity.Employee;
import com.example.multi_tanent.tenant.employee.repository.EmployeeRepository;
import com.example.multi_tanent.tenant.payroll.dto.PayrollInputDto;
import com.example.multi_tanent.tenant.payroll.entity.*;
import com.example.multi_tanent.tenant.payroll.enums.PayrollStatus;
import com.example.multi_tanent.tenant.payroll.enums.SalaryComponentType;
import com.example.multi_tanent.tenant.payroll.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(transactionManager = "tenantTx")
public class PayrollService {

    private final PayrollRunRepository payrollRunRepository;
    private final PayslipRepository payslipRepository;
    private final PayslipComponentRepository payslipComponentRepository;
    private final SalaryStructureRepository salaryStructureRepository;
    private final EmployeeRepository employeeRepository;
    private final SalaryComponentRepository salaryComponentRepository;

    public PayrollRun processPayroll(List<PayrollInputDto> inputs, int year, int month) {
        log.info("Processing payroll for {}/{}", month, year);

        // 1. Create or Get Payroll Run
        // Ideally checking if run already exists for this period to avoid duplicates or
        // to re-run
        // For simplicity, creating a new run or fetching existing pending one could be
        // logic.
        // Here we assume a new run for simplicity of the "run" action.

        PayrollRun payrollRun = new PayrollRun();
        payrollRun.setYear(year);
        payrollRun.setMonth(month);
        payrollRun.setPayPeriodStart(LocalDate.of(year, month, 1));
        payrollRun.setPayPeriodEnd(payrollRun.getPayPeriodStart().plusMonths(1).minusDays(1));
        payrollRun.setPayDate(LocalDate.now()); // Or configurable
        payrollRun.setStatus(PayrollStatus.PROCESSING);
        payrollRun.setExecutedAt(LocalDateTime.now());

        payrollRun = payrollRunRepository.save(payrollRun);

        List<Payslip> payslips = new ArrayList<>();

        for (PayrollInputDto input : inputs) {
            try {
                processEmployeePayslip(payrollRun, input, year, month);
            } catch (Exception e) {
                log.error("Error processing payroll for employee: {}", input.getEmployeeCode(), e);
                // Continue with other employees or throw exception based on requirement
            }
        }

        payrollRun.setStatus(PayrollStatus.COMPLETED);
        return payrollRunRepository.save(payrollRun);
    }

    private void processEmployeePayslip(PayrollRun payrollRun, PayrollInputDto input, int year, int month) {
        Employee employee = employeeRepository.findByEmployeeCode(input.getEmployeeCode())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + input.getEmployeeCode()));

        SalaryStructure salaryStructure = salaryStructureRepository.findByEmployeeId(employee.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Salary Structure not found for: " + input.getEmployeeCode()));

        Payslip payslip = new Payslip();
        payslip.setPayrollRun(payrollRun);
        payslip.setEmployee(employee);
        payslip.setYear(year);
        payslip.setMonth(month);
        payslip.setPayDate(payrollRun.getPayDate());
        payslip.setStatus(PayrollStatus.DRAFT);
        payslip.setTotalDaysInMonth(payrollRun.getPayPeriodEnd().lengthOfMonth());

        // Days calculation
        BigDecimal payableDays = input.getPayableDays() != null ? input.getPayableDays()
                : BigDecimal.valueOf(payslip.getTotalDaysInMonth());
        payslip.setPayableDays(payableDays);
        payslip.setLossOfPayDays(input.getLossOfPayDays() != null ? input.getLossOfPayDays() : BigDecimal.ZERO);

        // Calculate Components
        List<PayslipComponent> components = new ArrayList<>();
        BigDecimal grossEarnings = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;

        // 1. Fixed Components from Structure
        for (SalaryStructureComponent structComp : salaryStructure.getComponents()) {
            BigDecimal amount = structComp.getValue(); // Simplification: assuming fixed monthly value for now.
            // In real world, BASIC might depend on payableDays vs totalDays.
            // Let's apply Proration for Earnings if payableDays < totalDays

            if (structComp.getSalaryComponent().getType() == SalaryComponentType.EARNING &&
                    Boolean.TRUE.equals(structComp.getSalaryComponent().getIsPartOfGrossSalary())) { // Assuming logic
                // Proration logic: Amount * (Payable / Total)
                if (payslip.getPayableDays().compareTo(BigDecimal.valueOf(payslip.getTotalDaysInMonth())) < 0) {
                    amount = amount.multiply(payslip.getPayableDays())
                            .divide(BigDecimal.valueOf(payslip.getTotalDaysInMonth()), 2,
                                    java.math.RoundingMode.HALF_UP);
                }
                grossEarnings = grossEarnings.add(amount);
            } else if (structComp.getSalaryComponent().getType() == SalaryComponentType.DEDUCTION) {
                totalDeductions = totalDeductions.add(amount);
            }

            addPayslipComponent(payslip, structComp.getSalaryComponent(), amount, "Fixed", components);
        }

        // 2. Variable Components from Input
        if (input.getVariableComponents() != null) {
            for (Map.Entry<String, BigDecimal> entry : input.getVariableComponents().entrySet()) {
                String componentCode = entry.getKey();
                BigDecimal amount = entry.getValue();

                SalaryComponent salaryComponent = salaryComponentRepository.findByCode(componentCode)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid Component Code: " + componentCode));

                if (salaryComponent.getType() == SalaryComponentType.EARNING) {
                    grossEarnings = grossEarnings.add(amount);
                } else if (salaryComponent.getType() == SalaryComponentType.DEDUCTION) {
                    totalDeductions = totalDeductions.add(amount);
                }

                addPayslipComponent(payslip, salaryComponent, amount, "Variable", components);
            }
        }

        // 3. Explicit columns from Excel (Input)
        BigDecimal workExpenses = input.getWorkExpenses() != null ? input.getWorkExpenses() : BigDecimal.ZERO;
        BigDecimal netAdditions = input.getNetAdditions() != null ? input.getNetAdditions() : BigDecimal.ZERO;
        BigDecimal arrearsAddition = input.getArrearsAddition() != null ? input.getArrearsAddition() : BigDecimal.ZERO;
        BigDecimal arrearsDeduction = input.getArrearsDeduction() != null ? input.getArrearsDeduction()
                : BigDecimal.ZERO;

        payslip.setWorkExpenses(workExpenses);
        payslip.setNetAdditions(netAdditions);
        payslip.setArrearsAddition(arrearsAddition);
        payslip.setArrearsDeduction(arrearsDeduction);

        payslip.setGrossEarnings(grossEarnings);
        payslip.setTotalDeductions(totalDeductions);

        // Net Salary = Gross + Work Expenses + Net Additions + Arrears Addition - Total
        // Deductions - Arrears Deduction
        BigDecimal netSalary = grossEarnings
                .add(workExpenses)
                .add(netAdditions)
                .add(arrearsAddition)
                .subtract(totalDeductions)
                .subtract(arrearsDeduction);

        payslip.setNetSalary(netSalary);

        payslip = payslipRepository.save(payslip);

        for (PayslipComponent pc : components) {
            pc.setPayslip(payslip);
            payslipComponentRepository.save(pc);
        }
    }

    private void addPayslipComponent(Payslip payslip, SalaryComponent salaryComponent, BigDecimal amount,
            String remarks, List<PayslipComponent> list) {
        PayslipComponent pc = new PayslipComponent();
        pc.setPayslip(payslip);
        pc.setSalaryComponent(salaryComponent);
        pc.setAmount(amount);
        pc.setRemarks(remarks);
        list.add(pc);
    }
}
