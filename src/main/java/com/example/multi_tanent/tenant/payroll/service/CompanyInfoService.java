package com.example.multi_tanent.tenant.payroll.service;

import com.example.multi_tanent.config.TenantContext;
import com.example.multi_tanent.spersusers.enitity.CompanyInfo;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.tenant.payroll.dto.CompanyInfoRequest;
import com.example.multi_tanent.spersusers.repository.TenantRepository;
import com.example.multi_tanent.tenant.payroll.repository.CompanyInfoRepository;
import com.example.multi_tanent.tenant.service.FileStorageService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(transactionManager = "tenantTx")
public class CompanyInfoService {

    private final CompanyInfoRepository companyInfoRepository;
    private final FileStorageService fileStorageService;
    private final TenantRepository tenantRepository;

    public CompanyInfoService(CompanyInfoRepository companyInfoRepository,
            FileStorageService fileStorageService,
            TenantRepository tenantRepository) {
        this.companyInfoRepository = companyInfoRepository;
        this.fileStorageService = fileStorageService;
        this.tenantRepository = tenantRepository;
    }

    /**
     * Retrieves the company information for the current tenant.
     * As this is a singleton per tenant, it fetches the first available record.
     */
    public CompanyInfo getCompanyInfo() {
        // Find the first CompanyInfo record.
        CompanyInfo companyInfo = companyInfoRepository.findAll().stream().findFirst().orElse(null);
        if (companyInfo != null) {
            // Explicitly initialize lazy-loaded collections within the transaction to
            // prevent LazyInitializationException.
            companyInfo.getLocations().size();
            companyInfo.getBankAccounts().size();

            // Eagerly load tenant or set it if it's null (for existing records)
            if (companyInfo.getTenant() == null) {
                String tenantId = TenantContext.getTenantId();
                Tenant tenant = tenantRepository.findByTenantId(tenantId)
                        .orElseThrow(() -> new IllegalStateException("Tenant not found with ID: " + tenantId));
                companyInfo.setTenant(tenant);
                companyInfo = companyInfoRepository.save(companyInfo); // Save the updated association
            }
        }
        return companyInfo;
    }

    /**
     * Creates a new CompanyInfo record or updates the existing one.
     */
    public CompanyInfo createOrUpdateCompanyInfo(CompanyInfoRequest request) {
        CompanyInfo companyInfo = getCompanyInfo();
        if (companyInfo == null) {
            companyInfo = new CompanyInfo();
            // Set the tenant when creating for the first time
            String tenantIdStr = TenantContext.getTenantId();
            Tenant tenant = tenantRepository.findByTenantId(tenantIdStr)
                    .orElseThrow(
                            () -> new IllegalStateException("Cannot create CompanyInfo. Tenant not found with name: "));
            companyInfo.setTenant(tenant);
        }
        mapCompanyInfoRequestToEntity(request, companyInfo);
        return companyInfoRepository.save(companyInfo);
    }

    /**
     * Uploads and sets the company logo.
     * 
     * @param logoFile The logo file to upload.
     * @return The updated CompanyInfo entity.
     */
    public CompanyInfo uploadLogo(MultipartFile logoFile) {
        CompanyInfo companyInfo = getCompanyInfo();
        if (companyInfo == null) {
            throw new IllegalStateException("CompanyInfo must be created before uploading a logo.");
        }

        // If a logo already exists, delete the old one first.
        if (companyInfo.getLogoUrl() != null && !companyInfo.getLogoUrl().isEmpty()) {
            fileStorageService.deleteFile(companyInfo.getLogoUrl());
        }

        String filePath = fileStorageService.storeFile(logoFile, "logos");
        companyInfo.setLogoUrl(filePath);
        return companyInfoRepository.save(companyInfo);
    }

    public Resource getLogoAsResource() {
        CompanyInfo companyInfo = getCompanyInfo();
        if (companyInfo == null || companyInfo.getLogoUrl() == null || companyInfo.getLogoUrl().isEmpty()) {
            throw new EntityNotFoundException("Company logo has not been uploaded.");
        }
        return fileStorageService.loadFileAsResource(companyInfo.getLogoUrl());
    }

    private void mapCompanyInfoRequestToEntity(CompanyInfoRequest request, CompanyInfo entity) {
        entity.setCompanyName(request.getCompanyName());
        entity.setLogoUrl(request.getLogoUrl());
        entity.setAddress(request.getAddress());
        entity.setCity(request.getCity());
        entity.setEmirate(request.getEmirate());
        entity.setPoBox(request.getPoBox());
        entity.setCountry(request.getCountry());
        entity.setPhone(request.getPhone());
        entity.setEmail(request.getEmail());
        entity.setWebsite(request.getWebsite());

        entity.setTradeLicenseNumber(request.getTradeLicenseNumber());
        entity.setTradeLicenseExpiry(request.getTradeLicenseExpiry());
        entity.setTrn(request.getTrn());
        entity.setMohreEstablishmentId(request.getMohreEstablishmentId());
    }
}