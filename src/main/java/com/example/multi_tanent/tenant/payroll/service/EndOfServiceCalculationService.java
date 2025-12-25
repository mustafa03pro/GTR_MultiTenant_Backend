package com.example.multi_tanent.tenant.payroll.service;

import com.example.multi_tanent.config.TenantContext;
import com.example.multi_tanent.spersusers.enitity.CompanyInfo;
import com.example.multi_tanent.spersusers.enitity.Employee;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.spersusers.repository.TenantRepository;
import com.example.multi_tanent.tenant.employee.entity.JobDetails;
import com.example.multi_tanent.tenant.employee.repository.JobDetailsRepository;
import com.example.multi_tanent.tenant.employee.repository.EmployeeRepository;
import com.example.multi_tanent.tenant.payroll.entity.EndOfService;
import com.example.multi_tanent.tenant.payroll.dto.FinalSettlementPdfData;
import com.example.multi_tanent.spersusers.enums.ContractType;
import com.example.multi_tanent.tenant.payroll.enums.SalaryComponentType;
import com.example.multi_tanent.tenant.payroll.enums.TerminationReason;
import com.example.multi_tanent.tenant.payroll.entity.SalaryStructure;
import com.example.multi_tanent.tenant.payroll.repository.EndOfServiceRepository;
import com.example.multi_tanent.tenant.payroll.repository.SalaryStructureRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@Transactional(transactionManager = "tenantTx")
public class EndOfServiceCalculationService {

        private final EmployeeRepository employeeRepository;
        private final SalaryStructureRepository salaryStructureRepository;
        private final JobDetailsRepository jobDetailsRepository;
        private final EndOfServiceRepository endOfServiceRepository;
        private final EmployeeBenefitProvisionService provisionService;
        private final CompanyInfoService companyInfoService;
        private final TenantRepository tenantRepository;

        public EndOfServiceCalculationService(EmployeeRepository employeeRepository,
                        SalaryStructureRepository salaryStructureRepository,
                        JobDetailsRepository jobDetailsRepository,
                        EndOfServiceRepository endOfServiceRepository,
                        EmployeeBenefitProvisionService provisionService,
                        CompanyInfoService companyInfoService,
                        TenantRepository tenantRepository) {
                this.employeeRepository = employeeRepository;
                this.salaryStructureRepository = salaryStructureRepository;
                this.jobDetailsRepository = jobDetailsRepository;
                this.endOfServiceRepository = endOfServiceRepository;
                this.provisionService = provisionService;
                this.companyInfoService = companyInfoService;
                this.tenantRepository = tenantRepository;
        }

        /**
         * Calculates and saves the End of Service gratuity for an employee.
         * This is a simplified version based on common UAE labor law interpretations.
         *
         * @param employeeCode   The employee's code.
         * @param lastWorkingDay The employee's last day of work.
         * @param reason         The reason for the employee's departure.
         * @return The saved EndOfService entity.
         */
        public EndOfService calculateAndSaveGratuity(String employeeCode, LocalDate lastWorkingDay,
                        TerminationReason reason) {
                Employee employee = employeeRepository.findByEmployeeCode(employeeCode)
                                .orElseThrow(() -> new EntityNotFoundException("Employee not found: " + employeeCode));

                // Cancel any active, accruing benefit provisions for the departing employee
                provisionService.cancelProvisionsForEmployee(employee.getId());

                JobDetails jobDetails = jobDetailsRepository.findByEmployeeId(employee.getId())
                                .orElseThrow(() -> new IllegalStateException(
                                                "JobDetails not found for employee: " + employeeCode));

                LocalDate joiningDate = jobDetails.getDateOfJoining();
                if (joiningDate == null) {
                        throw new IllegalStateException("Employee joining date is not set in JobDetails.");
                }

                // --- Handle Termination for Cause (Article 44 / 120) ---
                // If terminated for gross misconduct, the employee is not entitled to gratuity.
                if (reason == TerminationReason.TERMINATION_FOR_CAUSE) {
                        long daysOfService = ChronoUnit.DAYS.between(joiningDate, lastWorkingDay);
                        BigDecimal yearsOfService = BigDecimal.valueOf(daysOfService).divide(BigDecimal.valueOf(365), 4,
                                        RoundingMode.HALF_UP);

                        EndOfService eos = endOfServiceRepository.findByEmployeeId(employee.getId())
                                        .orElse(new EndOfService());
                        eos.setEmployee(employee);
                        eos.setJoiningDate(joiningDate);
                        eos.setLastWorkingDay(lastWorkingDay);
                        eos.setTotalYearsOfService(yearsOfService); // Record years for auditing
                        eos.setLastBasicSalary(BigDecimal.ZERO); // Not applicable
                        eos.setGratuityAmount(BigDecimal.ZERO);
                        eos.setCalculationDetails(
                                        "Not entitled to gratuity due to termination for cause (as per UAE Labour Law Art. 44/120).");
                        eos.setTerminationReason(reason);
                        eos.setCalculatedAt(LocalDateTime.now());
                        return endOfServiceRepository.save(eos);
                }

                ContractType contractType = jobDetails.getContractType();
                if (contractType == null) {
                        throw new IllegalStateException("Employee contract type is not set in JobDetails.");
                }

                long daysOfService = ChronoUnit.DAYS.between(joiningDate, lastWorkingDay);
                BigDecimal yearsOfService = BigDecimal.valueOf(daysOfService).divide(BigDecimal.valueOf(365), 4,
                                RoundingMode.HALF_UP);

                if (yearsOfService.compareTo(BigDecimal.ONE) < 0) {
                        // No gratuity for less than 1 year of service
                        return null;
                }

                SalaryStructure structure = salaryStructureRepository.findByEmployeeId(employee.getId())
                                .orElseThrow(
                                                () -> new IllegalStateException(
                                                                "Salary structure not found for employee: "
                                                                                + employeeCode));

                BigDecimal lastBasicSalary = structure.getComponents().stream()
                                .filter(c -> "BASIC".equalsIgnoreCase(c.getSalaryComponent().getCode()))
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException(
                                                "Basic salary component not found in structure."))
                                .getValue();

                BigDecimal dailyBasic = lastBasicSalary.divide(BigDecimal.valueOf(30), 4, RoundingMode.HALF_UP);
                BigDecimal gratuityAmount;
                String details;

                if (yearsOfService.compareTo(BigDecimal.valueOf(5)) < 0) {
                        // 21 days of basic salary for each of the first five years
                        gratuityAmount = dailyBasic.multiply(BigDecimal.valueOf(21)).multiply(yearsOfService);
                        details = String.format("%.2f years * 21 days * AED %.2f (daily basic)", yearsOfService,
                                        dailyBasic);
                } else {
                        // 21 days for first 5 years + 30 days for each additional year
                        BigDecimal firstFiveYearsGratuity = dailyBasic.multiply(BigDecimal.valueOf(21))
                                        .multiply(BigDecimal.valueOf(5));
                        BigDecimal remainingYears = yearsOfService.subtract(BigDecimal.valueOf(5));
                        BigDecimal remainingYearsGratuity = dailyBasic.multiply(BigDecimal.valueOf(30))
                                        .multiply(remainingYears);
                        gratuityAmount = firstFiveYearsGratuity.add(remainingYearsGratuity);
                        details = String.format("(5 years * 21 days) + (%.2f years * 30 days) * AED %.2f (daily basic)",
                                        remainingYears, dailyBasic);
                }

                // --- Apply Reductions based on Contract Type and Resignation (Historical Rule)
                // ---
                // Note: The new UAE labor law has largely removed these reductions. This is for
                // demonstration.
                if (contractType == ContractType.UNLIMITED && reason == TerminationReason.RESIGNATION) {
                        if (yearsOfService.compareTo(BigDecimal.valueOf(3)) < 0) { // 1 to 3 years
                                gratuityAmount = gratuityAmount
                                                .multiply(BigDecimal.ONE.divide(BigDecimal.valueOf(3), 4,
                                                                RoundingMode.HALF_UP));
                                details += " (Reduced to 1/3 for resignation under 3 years)";
                        } else if (yearsOfService.compareTo(BigDecimal.valueOf(5)) < 0) { // 3 to 5 years
                                gratuityAmount = gratuityAmount
                                                .multiply(BigDecimal.valueOf(2).divide(BigDecimal.valueOf(3), 4,
                                                                RoundingMode.HALF_UP));
                                details += " (Reduced to 2/3 for resignation between 3-5 years)";
                        }
                        // No reduction for resignation after 5 years.
                }

                // --- Apply Gratuity Cap as per UAE Law ---
                // The cap is based on two years' total salary.
                BigDecimal lastGrossSalary = structure.getComponents().stream()
                                .filter(c -> c.getSalaryComponent().getType() == SalaryComponentType.EARNING
                                                && Boolean.TRUE.equals(c.getSalaryComponent().getIsPartOfGrossSalary()))
                                .map(c -> c.getValue() != null ? c.getValue() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal twoYearsSalaryCap = lastGrossSalary.multiply(BigDecimal.valueOf(24));

                if (gratuityAmount.compareTo(twoYearsSalaryCap) > 0) {
                        gratuityAmount = twoYearsSalaryCap;
                        details += " (Capped at 2 years' salary: AED "
                                        + twoYearsSalaryCap.setScale(2, RoundingMode.HALF_UP) + ")";
                }

                // This is a simplified calculation. UAE law has caps (e.g., total gratuity not
                // exceeding 2 years' salary)
                // and reductions based on resignation circumstances, which should be added
                // here.

                EndOfService eos = endOfServiceRepository.findByEmployeeId(employee.getId()).orElse(new EndOfService());
                eos.setEmployee(employee);
                eos.setJoiningDate(joiningDate);
                eos.setLastWorkingDay(lastWorkingDay);
                eos.setTotalYearsOfService(yearsOfService);
                eos.setLastBasicSalary(lastBasicSalary);
                eos.setGratuityAmount(gratuityAmount.setScale(2, RoundingMode.HALF_UP));
                eos.setCalculationDetails(details);
                eos.setTerminationReason(reason);
                eos.setCalculatedAt(LocalDateTime.now());

                return endOfServiceRepository.save(eos);
        }

        @Transactional(readOnly = true)
        public Optional<EndOfService> getEndOfServiceByEmployeeCode(String employeeCode) {
                return employeeRepository.findByEmployeeCode(employeeCode)
                                .flatMap(employee -> endOfServiceRepository.findByEmployeeId(employee.getId()));
        }

        @Transactional(readOnly = true)
        public Optional<FinalSettlementPdfData> getFinalSettlementDataForPdf(String employeeCode) {
                return getEndOfServiceByEmployeeCode(employeeCode).map(eos -> {
                        Long employeeId = eos.getEmployee().getId();

                        CompanyInfo companyInfo = companyInfoService.getCompanyInfo();
                        Tenant tenant = tenantRepository.findByTenantId(TenantContext.getTenantId())
                                        .orElseThrow(() -> new IllegalStateException(
                                                        "Tenant not found for context: "
                                                                        + TenantContext.getTenantId()));
                        JobDetails jobDetails = jobDetailsRepository.findByEmployeeId(employeeId)
                                        .orElse(new JobDetails());

                        return new FinalSettlementPdfData(eos, companyInfo, tenant, jobDetails);
                });
        }
}