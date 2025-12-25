package com.example.multi_tanent.production.controller;

import com.example.multi_tanent.production.dto.ManufacturingOrderFileResponse;
import com.example.multi_tanent.production.dto.ManufacturingOrderRequest;
import com.example.multi_tanent.production.dto.ManufacturingOrderResponse;
import com.example.multi_tanent.production.services.ManufacturingOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/production/manufacturing-orders")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ManufacturingOrderController {

    private final ManufacturingOrderService service;

    @PostMapping
    public ResponseEntity<ManufacturingOrderResponse> create(@Valid @RequestBody ManufacturingOrderRequest request) {
        return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<ManufacturingOrderResponse>> getAll(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) com.example.multi_tanent.production.enums.ManufacturingOrderStatus status,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate toDate) {
        return ResponseEntity.ok(service.getAll(pageable, search, status, fromDate, toDate));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ManufacturingOrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ManufacturingOrderResponse> update(@PathVariable Long id,
            @Valid @RequestBody ManufacturingOrderRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/files")
    public ResponseEntity<ManufacturingOrderFileResponse> uploadFile(@PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(service.uploadFile(id, file));
    }

    @PostMapping("/from-sales-order/{salesOrderId}")
    public ResponseEntity<java.util.List<ManufacturingOrderResponse>> createFromSalesOrder(
            @PathVariable Long salesOrderId) {
        return new ResponseEntity<>(service.createFromSalesOrder(salesOrderId), HttpStatus.CREATED);
    }

    @GetMapping("/{id}/export-bom")
    public ResponseEntity<org.springframework.core.io.Resource> exportBom(@PathVariable Long id) {
        org.springframework.core.io.InputStreamResource file = new org.springframework.core.io.InputStreamResource(
                service.exportBom(id));
        String filename = "BOM_Export_" + id + ".xlsx";
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(org.springframework.http.MediaType
                        .parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }

    @GetMapping("/files/{fileId}")
    public ResponseEntity<org.springframework.core.io.Resource> viewFile(@PathVariable Long fileId) {
        org.springframework.core.io.Resource resource = service.getFileResource(fileId);
        String contentType = "application/octet-stream";
        try {
            contentType = java.nio.file.Files
                    .probeContentType(java.nio.file.Paths.get(resource.getFile().getAbsolutePath()));
        } catch (java.io.IOException ex) {
            // fallback to default
        }

        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
