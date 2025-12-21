package com.example.multi_tanent.tenant.payroll.controller;

import com.example.multi_tanent.spersusers.enitity.CompanyInfo;
import com.example.multi_tanent.tenant.payroll.dto.CompanyInfoRequest;
import com.example.multi_tanent.tenant.payroll.dto.CompanyInfoResponse;
import com.example.multi_tanent.tenant.payroll.service.CompanyInfoService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/company-info")
@CrossOrigin(origins = "*")
public class CompanyInfoController {

    private final CompanyInfoService companyInfoService;

    public CompanyInfoController(CompanyInfoService companyInfoService) {
        this.companyInfoService = companyInfoService;
    }

    @GetMapping
    public ResponseEntity<CompanyInfoResponse> getCompanyInfo() {
        CompanyInfo companyInfo = companyInfoService.getCompanyInfo();
        return ResponseEntity.ok(CompanyInfoResponse.fromEntity(companyInfo));
    }

    @PostMapping
    public ResponseEntity<CompanyInfoResponse> createOrUpdateCompanyInfo(@RequestBody CompanyInfoRequest request) {
        CompanyInfo companyInfo = companyInfoService.createOrUpdateCompanyInfo(request);
        return ResponseEntity.ok(CompanyInfoResponse.fromEntity(companyInfo));
    }

    @PutMapping
    public ResponseEntity<CompanyInfoResponse> updateCompanyInfo(@RequestBody CompanyInfoRequest request) {
        CompanyInfo companyInfo = companyInfoService.createOrUpdateCompanyInfo(request);
        return ResponseEntity.ok(CompanyInfoResponse.fromEntity(companyInfo));
    }

    @PostMapping("/logo")
    public ResponseEntity<CompanyInfoResponse> uploadLogo(@RequestParam("file") MultipartFile file) {
        CompanyInfo companyInfo = companyInfoService.uploadLogo(file);
        return ResponseEntity.ok(CompanyInfoResponse.fromEntity(companyInfo));
    }

    @GetMapping("/logo")
    public ResponseEntity<Resource> getLogo() {
        Resource resource = companyInfoService.getLogoAsResource();
        String contentType;
        try {
            contentType = resource.getURL().openConnection().getContentType();
        } catch (IOException e) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}
