package com.example.multi_tanent.pos.service;

import com.example.multi_tanent.config.TenantContext;
import com.example.multi_tanent.pos.dto.UpdateTenantRequest;
import com.example.multi_tanent.spersusers.dto.TenantDto;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.spersusers.repository.TenantRepository;
import com.example.multi_tanent.pos.service.FileStorageService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
@Transactional(transactionManager = "tenantTx")
public class TenantService {
    private final TenantRepository tenantRepository;
    private final FileStorageService fileStorageService;

    public TenantService(TenantRepository tenantRepository,
            @Qualifier("posFileStorageService") FileStorageService fileStorageService) {
        this.tenantRepository = tenantRepository;
        this.fileStorageService = fileStorageService;
    }

    public Optional<Tenant> getCurrentTenant() {
        // Correctly fetch the single tenant record from the tenant's database.
        return tenantRepository.findAll().stream().findFirst();
    }

    public Tenant updateCurrentTenant(UpdateTenantRequest updateRequest) {
        Tenant tenant = getCurrentTenant()
                .orElseThrow(() -> new IllegalStateException("Tenant not found in current context."));
        if (updateRequest.getName() != null)
            tenant.setName(updateRequest.getName());
        if (updateRequest.getContactEmail() != null)
            tenant.setContactEmail(updateRequest.getContactEmail());
        if (updateRequest.getContactPhone() != null)
            tenant.setContactPhone(updateRequest.getContactPhone());
        if (updateRequest.getAddress() != null)
            tenant.setAddress(updateRequest.getAddress());

        // Update SMTP settings
        if (updateRequest.getSmtpHost() != null)
            tenant.setSmtpHost(updateRequest.getSmtpHost());
        if (updateRequest.getSmtpPort() != null)
            tenant.setSmtpPort(updateRequest.getSmtpPort());
        if (updateRequest.getSmtpUsername() != null)
            tenant.setSmtpUsername(updateRequest.getSmtpUsername());
        if (updateRequest.getSmtpPassword() != null)
            tenant.setSmtpPassword(updateRequest.getSmtpPassword());
        if (updateRequest.getCompanyEmail() != null)
            tenant.setCompanyEmail(updateRequest.getCompanyEmail());
        return tenantRepository.save(tenant);
    }

    public Tenant updateTenantLogo(MultipartFile file) {
        Tenant tenant = getCurrentTenant()
                .orElseThrow(() -> new IllegalStateException("Tenant not found in current context."));
        String fileName = fileStorageService.storeFile(file, "logo");
        tenant.setLogoImgUrl(fileName);
        return tenantRepository.save(tenant);
    }

    public TenantDto toDto(Tenant tenant) {
        TenantDto dto = new TenantDto();
        dto.setId(tenant.getId());
        dto.setName(tenant.getName());
        dto.setLogoImgUrl(tenant.getLogoImgUrl());
        dto.setContactEmail(tenant.getContactEmail());
        dto.setContactPhone(tenant.getContactPhone());
        dto.setAddress(tenant.getAddress());

        // Include SMTP settings in the DTO
        dto.setSmtpHost(tenant.getSmtpHost());
        dto.setSmtpPort(tenant.getSmtpPort());
        dto.setSmtpUsername(tenant.getSmtpUsername());
        // Password is intentionally omitted for security
        dto.setCompanyEmail(tenant.getCompanyEmail());
        return dto;
    }

    public Tenant getTenantFromRequest(jakarta.servlet.http.HttpServletRequest request) {
        return getCurrentTenant().orElseThrow(() -> new IllegalStateException("Tenant not found"));
    }
}