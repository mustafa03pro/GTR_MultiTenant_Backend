package com.example.multi_tanent.tenant.payroll.service;

import com.example.multi_tanent.spersusers.enitity.CompanyInfo;
import com.example.multi_tanent.spersusers.enitity.Employee;
import com.example.multi_tanent.tenant.employee.repository.EmployeeRepository;
import com.example.multi_tanent.tenant.payroll.dto.EmployeeBenefitProvisionRequest;
import com.example.multi_tanent.tenant.payroll.dto.BenefitVoucherPdfData;
import com.example.multi_tanent.tenant.payroll.dto.ProvisionPayoutRequest;
import com.example.multi_tanent.tenant.payroll.entity.BenefitPayoutFile;
import com.example.multi_tanent.tenant.payroll.entity.BenefitType;
import com.example.multi_tanent.tenant.payroll.repository.BenefitTypeRepository;
import com.example.multi_tanent.tenant.payroll.entity.EmployeeBenefitProvision;
import com.example.multi_tanent.tenant.payroll.enums.ProvisionStatus;
import com.example.multi_tanent.tenant.payroll.repository.EmployeeBenefitProvisionRepository;
import com.example.multi_tanent.tenant.service.FileStorageService;

import org.springframework.core.io.Resource;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(transactionManager = "tenantTx")
public class EmployeeBenefitProvisionService {

    private final EmployeeBenefitProvisionRepository provisionRepository;
    private final EmployeeRepository employeeRepository;
    private final BenefitTypeRepository benefitTypeRepository;
    private final FileStorageService fileStorageService;
    private final CompanyInfoService companyInfoService;

    public EmployeeBenefitProvisionService(EmployeeBenefitProvisionRepository provisionRepository,
            EmployeeRepository employeeRepository,
            BenefitTypeRepository benefitTypeRepository,
            FileStorageService fileStorageService,
            CompanyInfoService companyInfoService) {
        this.provisionRepository = provisionRepository;
        this.employeeRepository = employeeRepository;
        this.benefitTypeRepository = benefitTypeRepository;
        this.fileStorageService = fileStorageService;
        this.companyInfoService = companyInfoService;
    }

    public EmployeeBenefitProvision createProvision(EmployeeBenefitProvisionRequest request) {
        Employee employee = employeeRepository.findByEmployeeCode(request.getEmployeeCode())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Employee not found with code: " + request.getEmployeeCode()));

        BenefitType benefitType = benefitTypeRepository.findById(request.getBenefitTypeId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "BenefitType not found with id: " + request.getBenefitTypeId()));

        EmployeeBenefitProvision provision = new EmployeeBenefitProvision();
        provision.setEmployee(employee);
        provision.setBenefitType(benefitType);
        provision.setCycleStartDate(request.getCycleStartDate());
        provision.setCycleEndDate(request.getCycleEndDate());
        provision.setAccruedAmount(BigDecimal.ZERO); // Initial accrued amount is zero
        provision.setStatus(ProvisionStatus.ACCRUING); // Starts in ACCRUING state

        return provisionRepository.save(provision);
    }

    @Transactional(readOnly = true)
    public EmployeeBenefitProvision getProvisionById(Long id) {
        // Use the new method to prevent LazyInitializationException
        return provisionRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Benefit provision not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<EmployeeBenefitProvision> getProvisionsByEmployeeCode(String employeeCode) {
        if (!employeeRepository.existsByEmployeeCode(employeeCode)) {
            throw new EntityNotFoundException("Employee not found with code: " + employeeCode);
        }
        return provisionRepository.findByEmployeeEmployeeCodeWithDetails(employeeCode);
    }

    public EmployeeBenefitProvision updateProvision(Long id, EmployeeBenefitProvisionRequest request) {
        EmployeeBenefitProvision provision = getProvisionById(id);

        // You can't change the employee associated with a provision
        if (!provision.getEmployee().getEmployeeCode().equals(request.getEmployeeCode())) {
            throw new IllegalArgumentException("Cannot change the employee of an existing provision.");
        }

        BenefitType benefitType = benefitTypeRepository.findById(request.getBenefitTypeId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "BenefitType not found with id: " + request.getBenefitTypeId()));

        provision.setBenefitType(benefitType);
        provision.setCycleStartDate(request.getCycleStartDate());
        provision.setCycleEndDate(request.getCycleEndDate());
        // Note: Accrued amount and status are managed by the system, not updated here.

        return provisionRepository.save(provision);
    }

    public void deleteProvision(Long id) {
        provisionRepository.deleteById(id);
    }

    public EmployeeBenefitProvision markAsPaidOut(Long provisionId, ProvisionPayoutRequest request,
            MultipartFile[] files) {
        EmployeeBenefitProvision provision = getProvisionById(provisionId);
        if (provision.getStatus() != ProvisionStatus.ACCRUING) {
            throw new IllegalStateException(
                    "Provision can only be paid out from ACCRUING status. Current status: " + provision.getStatus());
        }

        // Validate that the amount being paid out matches the accrued amount.
        // This prevents partial or incorrect payouts.
        if (provision.getAccruedAmount().compareTo(request.getPaidAmount()) != 0) {
            throw new IllegalStateException("Paid amount (" + request.getPaidAmount()
                    + ") does not match the accrued amount (" + provision.getAccruedAmount() + ").");
        }

        if (files != null) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String filePath = fileStorageService.storeFile(file, "benefit-confirmations");
                    BenefitPayoutFile payoutFile = new BenefitPayoutFile();
                    payoutFile.setProvision(provision);
                    payoutFile.setFilePath(filePath);
                    payoutFile.setOriginalFilename(file.getOriginalFilename());
                    provision.getConfirmationFiles().add(payoutFile);
                }
            }
        }

        provision.setStatus(ProvisionStatus.PAID_OUT);
        provision.setPaymentMethod(request.getPaymentMethod());
        provision.setPaidAmount(request.getPaidAmount());
        provision.setPaidOutDate(request.getPaidOutDate());
        provision.setPaymentDetails(request.getPaymentDetails());
        return provisionRepository.save(provision);
    }

    public void cancelProvisionsForEmployee(Long employeeId) {
        List<EmployeeBenefitProvision> accruingProvisions = provisionRepository.findByEmployeeIdAndStatus(employeeId,
                ProvisionStatus.ACCRUING);
        for (EmployeeBenefitProvision provision : accruingProvisions) {
            provision.setStatus(ProvisionStatus.CANCELLED);
            // Note: In a full accounting system, you would also create a journal entry
            // to reverse the accrued amount from your liability account.
        }
        provisionRepository.saveAll(accruingProvisions);
    }

    public Resource loadConfirmationFile(Long provisionId) {
        EmployeeBenefitProvision provision = getProvisionById(provisionId);
        // Assuming there's only one confirmation file for simplicity, or you need to
        // specify which one
        // This needs to be adapted if multiple files are possible and you want to
        // retrieve a specific one.
        if (provision.getConfirmationFiles().isEmpty()) {
            throw new EntityNotFoundException("No confirmation file found for provision id: " + provisionId);
        }
        return fileStorageService.loadFileAsResource(provision.getConfirmationFiles().get(0).getFilePath());
    }

    @Transactional(readOnly = true)
    public Optional<BenefitVoucherPdfData> getDataForVoucherPdf(Long provisionId) {
        return provisionRepository.findByIdWithDetails(provisionId).map(provision -> {
            CompanyInfo companyInfo = companyInfoService.getCompanyInfo();
            // The provision object is already fully loaded by findByIdWithDetails
            return new BenefitVoucherPdfData(provision, companyInfo);
        });
    }
}