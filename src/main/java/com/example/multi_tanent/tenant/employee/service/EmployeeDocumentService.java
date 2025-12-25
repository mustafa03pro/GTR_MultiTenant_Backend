package com.example.multi_tanent.tenant.employee.service;

import com.example.multi_tanent.spersusers.enitity.Employee;
import com.example.multi_tanent.tenant.base.entity.DocumentType;
import com.example.multi_tanent.tenant.base.repository.DocumentTypeRepository;
import com.example.multi_tanent.tenant.employee.entity.EmployeeDocument;
import com.example.multi_tanent.tenant.employee.repository.EmployeeDocumentRepository;
import com.example.multi_tanent.tenant.employee.repository.EmployeeRepository;
import com.example.multi_tanent.tenant.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.example.multi_tanent.config.TenantContext;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(transactionManager = "tenantTx")
public class EmployeeDocumentService {

    private final EmployeeDocumentRepository documentRepository;
    private final EmployeeRepository employeeRepository;
    private final FileStorageService fileStorageService;
    private final DocumentTypeRepository documentTypeRepository;

    public EmployeeDocumentService(EmployeeDocumentRepository documentRepository,
            EmployeeRepository employeeRepository,
            FileStorageService fileStorageService,
            DocumentTypeRepository documentTypeRepository) {
        this.documentRepository = documentRepository;
        this.employeeRepository = employeeRepository;
        this.fileStorageService = fileStorageService;
        this.documentTypeRepository = documentTypeRepository;
    }

    public EmployeeDocument storeDocument(MultipartFile file, String employeeCode, Long docTypeId, String documentId,
            LocalDate registrationDate, LocalDate endDate, String remarks) {
        Employee employee = employeeRepository.findByEmployeeCode(employeeCode)
                .orElseThrow(() -> new RuntimeException("Employee not found with code: " + employeeCode));

        DocumentType documentType = documentTypeRepository.findById(docTypeId)
                .orElseThrow(() -> new RuntimeException("DocumentType not found with id: " + docTypeId));

        String fileName = fileStorageService.storeFile(file, employeeCode);

        EmployeeDocument doc = new EmployeeDocument();
        doc.setEmployee(employee);
        doc.setDocumentType(documentType);
        doc.setDocumentId(documentId);
        doc.setRegistrationDate(registrationDate);
        doc.setEndDate(endDate);
        doc.setFileName(fileName);
        doc.setFilePath(fileStorageService.getFileStorageLocation().resolve(fileName).toString());
        doc.setRemarks(remarks);
        doc.setVerified(false);

        return documentRepository.save(doc);
    }

    @Transactional(readOnly = true)
    public Optional<EmployeeDocument> getDocument(Long fileId) {
        return documentRepository.findById(fileId);
    }

    @Transactional(readOnly = true)
    public List<EmployeeDocument> getDocumentsForEmployee(String employeeCode) {
        employeeRepository.findByEmployeeCode(employeeCode)
                .orElseThrow(() -> new RuntimeException("Employee not found with code: " + employeeCode));
        return documentRepository.findByEmployeeEmployeeCode(employeeCode);
    }

    public Resource loadFile(String fileName) {
        return fileStorageService.loadFileAsResource(fileName);
    }

    public void deleteDocument(Long documentId) {
        EmployeeDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + documentId));

        fileStorageService.deleteFile(doc.getFileName());
        documentRepository.delete(doc);
    }

    public EmployeeDocument updateDocumentDetails(Long documentId, Long docTypeId, String newDocumentId,
            LocalDate registrationDate, LocalDate endDate, String remarks, Boolean verified) {
        EmployeeDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + documentId));

        DocumentType documentType = documentTypeRepository.findById(docTypeId)
                .orElseThrow(() -> new RuntimeException("DocumentType not found with id: " + docTypeId));

        doc.setDocumentType(documentType);
        doc.setDocumentId(newDocumentId);
        doc.setRegistrationDate(registrationDate);
        doc.setEndDate(endDate);
        doc.setRemarks(remarks);
        doc.setVerified(verified);

        return documentRepository.save(doc);
    }
}