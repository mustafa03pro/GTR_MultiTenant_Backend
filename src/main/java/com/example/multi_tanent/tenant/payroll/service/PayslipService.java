package com.example.multi_tanent.tenant.payroll.service;

import com.example.multi_tanent.spersusers.enitity.CompanyInfo;
import com.example.multi_tanent.tenant.employee.entity.EmployeeProfile;
import com.example.multi_tanent.tenant.employee.entity.JobDetails;
import com.example.multi_tanent.tenant.employee.repository.EmployeeProfileRepository;
import com.example.multi_tanent.tenant.employee.repository.JobDetailsRepository;
import com.example.multi_tanent.tenant.payroll.dto.PayslipPdfData;
import com.example.multi_tanent.tenant.payroll.entity.Payslip;
import com.example.multi_tanent.tenant.payroll.entity.PayslipComponent;
import com.example.multi_tanent.tenant.payroll.repository.PayslipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional(transactionManager = "tenantTx")
public class PayslipService {

    private final PayslipRepository payslipRepository;
    private final CompanyInfoService companyInfoService;
    private final JobDetailsRepository jobDetailsRepository;
    private final EmployeeProfileRepository employeeProfileRepository;

    public PayslipService(PayslipRepository payslipRepository,
            CompanyInfoService companyInfoService,
            JobDetailsRepository jobDetailsRepository,
            EmployeeProfileRepository employeeProfileRepository) {
        this.payslipRepository = payslipRepository;
        this.companyInfoService = companyInfoService;
        this.jobDetailsRepository = jobDetailsRepository;
        this.employeeProfileRepository = employeeProfileRepository;
    }

    public List<Payslip> getPayslipsForEmployee(String employeeCode) {
        List<Payslip> payslips = payslipRepository.findByEmployeeEmployeeCodeOrderByYearDescMonthDesc(employeeCode);
        // Eagerly initialize the necessary associations within the transaction
        payslips.forEach(this::initializePayslipDetails);
        return payslips;
    }

    public Optional<Payslip> getPayslipById(Long id) {
        // Use the new query to fetch everything in one go, preventing
        // LazyInitializationException
        Optional<Payslip> payslipOpt = payslipRepository.findByIdWithDetails(id);
        return payslipOpt;
    }

    public Optional<PayslipPdfData> getPayslipDataForPdf(Long payslipId) {
        return getPayslipById(payslipId).map(payslip -> {
            Long employeeId = payslip.getEmployee().getId();

            CompanyInfo companyInfo = companyInfoService.getCompanyInfo();
            if (companyInfo == null) {
                throw new IllegalStateException(
                        "Company Information is not configured for this tenant. Please set it up before generating payslips.");
            }

            JobDetails jobDetails = jobDetailsRepository.findByEmployeeId(employeeId)
                    .orElse(new JobDetails());
            EmployeeProfile employeeProfile = employeeProfileRepository.findByEmployeeId(employeeId)
                    .orElse(new EmployeeProfile());

            return new PayslipPdfData(payslip, companyInfo, companyInfo.getTenant(), jobDetails, employeeProfile);
        });
    }

    public List<Payslip> getPayslipsForPayrollRun(Long payrollRunId) {
        List<Payslip> payslips = payslipRepository.findByPayrollRunId(payrollRunId);
        payslips.forEach(this::initializePayslipDetails);
        return payslips;
    }

    /**
     * Initializes lazy-loaded associations of a Payslip entity to prevent
     * LazyInitializationException.
     * This should be called within a @Transactional context.
     * 
     * @param payslip The Payslip entity to initialize.
     */
    private void initializePayslipDetails(Payslip payslip) {
        if (payslip == null)
            return;

        // Initialize the Employee proxy by accessing a property
        if (payslip.getEmployee() != null) {
            payslip.getEmployee().getEmployeeCode();
        }

        // Initialize the components and their associated SalaryComponent
        if (payslip.getComponents() != null) {
            payslip.getComponents().stream()
                    .map(PayslipComponent::getSalaryComponent)
                    .filter(Objects::nonNull)
                    .forEach(sc -> sc.getCode()); // Access a property to trigger load
        }
    }
}
