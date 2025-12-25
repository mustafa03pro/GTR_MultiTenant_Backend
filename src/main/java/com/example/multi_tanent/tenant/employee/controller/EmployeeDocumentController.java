package com.example.multi_tanent.tenant.employee.controller;

import com.example.multi_tanent.tenant.employee.dto.EmployeeDocumentRequest;
import com.example.multi_tanent.tenant.employee.entity.EmployeeDocument;
import com.example.multi_tanent.tenant.employee.service.EmployeeDocumentService;
import com.example.multi_tanent.tenant.reports.service.HrmsReportService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/employee-documents")
@CrossOrigin(origins = "*", exposedHeaders = "Content-Disposition")
@Transactional(transactionManager = "tenantTx")
public class EmployeeDocumentController {

    private final EmployeeDocumentService documentService;
    private static final Logger logger = LoggerFactory.getLogger(EmployeeDocumentController.class);

    private final HrmsReportService hrmsReportService;

    public EmployeeDocumentController(EmployeeDocumentService documentService, HrmsReportService hrmsReportService) {
        this.documentService = documentService;
        this.hrmsReportService = hrmsReportService;
    }

    @PostMapping(path = "/{employeeCode}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HRMS_ADMIN','HR','MANAGER')")
    public ResponseEntity<EmployeeDocument> uploadDocument(@PathVariable String employeeCode,
            @RequestParam("file") MultipartFile file,
            @RequestParam("docTypeId") Long docTypeId,
            @RequestParam(value = "documentId", required = false) String documentId,
            @RequestParam(value = "registrationDate", required = false) LocalDate registrationDate,
            @RequestParam(value = "endDate", required = false) LocalDate endDate,
            @RequestParam(value = "remarks", required = false) String remarks) {
        EmployeeDocument savedDoc = documentService.storeDocument(file, employeeCode, docTypeId, documentId,
                registrationDate, endDate, remarks);

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/employee-documents/by-id/{id}")
                .buildAndExpand(savedDoc.getId()).toUri();
        return ResponseEntity.created(location).body(savedDoc);
    }

    @GetMapping("/{employeeCode}")
    public ResponseEntity<List<EmployeeDocument>> getDocumentsForEmployee(@PathVariable String employeeCode) {
        return ResponseEntity.ok(documentService.getDocumentsForEmployee(employeeCode));
    }

    @GetMapping("/by-id/{id}")
    public ResponseEntity<EmployeeDocument> getDocumentById(@PathVariable Long id) {
        return documentService.getDocument(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/download/{documentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HRMS_ADMIN','HR','MANAGER') or @documentSecurityService.isOwner(#documentId, authentication)")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long documentId) {
        return documentService.getDocument(documentId)
                .map(doc -> {
                    Resource resource = documentService.loadFile(doc.getFileName());
                    String contentType = "application/octet-stream";
                    try {
                        Path path = resource.getFile().toPath();
                        contentType = Files.probeContentType(path);
                    } catch (IOException ex) {
                        logger.warn("Could not determine content type for file: {}", resource.getFilename(), ex);
                    }
                    if (contentType == null) {
                        contentType = "application/octet-stream";
                    }

                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(contentType))
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    "attachment; filename=\"" + resource.getFilename() + "\"")
                            .body(resource);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/view/{documentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HRMS_ADMIN','HR','MANAGER') or @documentSecurityService.isOwner(#documentId, authentication)")
    public ResponseEntity<Resource> viewFile(@PathVariable Long documentId) {
        return documentService.getDocument(documentId)
                .map(doc -> {
                    Resource resource = documentService.loadFile(doc.getFileName());
                    String contentType = "application/octet-stream";
                    try {
                        Path path = resource.getFile().toPath();
                        contentType = Files.probeContentType(path);
                    } catch (IOException ex) {
                        logger.warn("Could not determine content type for file: {}", resource.getFilename(), ex);
                    }
                    if (contentType == null) {
                        contentType = "application/octet-stream";
                    }

                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(contentType))
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    "inline; filename=\"" + resource.getFilename() + "\"")
                            .body(resource);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{documentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HRMS_ADMIN','HR','MANAGER')")
    public ResponseEntity<EmployeeDocument> updateDocumentDetails(@PathVariable Long documentId,
            @RequestBody EmployeeDocumentRequest request) {
        EmployeeDocument updatedDoc = documentService.updateDocumentDetails(documentId, request.getDocTypeId(),
                request.getDocumentId(), request.getRegistrationDate(), request.getEndDate(),
                request.getRemarks(), request.getVerified());
        return ResponseEntity.ok(updatedDoc);
    }

    @DeleteMapping("/{documentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HRMS_ADMIN','HR','MANAGER')")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long documentId) {
        documentService.deleteDocument(documentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HRMS_ADMIN','HR','MANAGER')")
    public ResponseEntity<Resource> exportDocuments() {
        String tenantId = com.example.multi_tanent.config.TenantContext.getTenantId();
        org.springframework.core.io.InputStreamResource file = new org.springframework.core.io.InputStreamResource(
                hrmsReportService.generateEmployeeDocumentReport(tenantId));
        String filename = "Employee_Documents_Report.xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }
}
