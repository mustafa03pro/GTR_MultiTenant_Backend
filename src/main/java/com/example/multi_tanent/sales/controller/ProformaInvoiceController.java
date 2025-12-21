package com.example.multi_tanent.sales.controller;

import com.example.multi_tanent.sales.dto.ProformaInvoiceRequest;
import com.example.multi_tanent.sales.dto.ProformaInvoiceResponse;
import com.example.multi_tanent.sales.enums.SalesStatus;
import com.example.multi_tanent.sales.service.ProformaInvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/sales/proforma-invoices")
@RequiredArgsConstructor
public class ProformaInvoiceController {

    private final ProformaInvoiceService proformaInvoiceService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProformaInvoiceResponse> createProformaInvoice(
            @RequestPart("proformaInvoice") ProformaInvoiceRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return ResponseEntity.ok(proformaInvoiceService.createProformaInvoice(request, files));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProformaInvoiceResponse> updateProformaInvoice(
            @PathVariable Long id,
            @RequestPart("proformaInvoice") ProformaInvoiceRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return ResponseEntity.ok(proformaInvoiceService.updateProformaInvoice(id, request, files));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProformaInvoiceResponse> getProformaInvoiceById(@PathVariable Long id) {
        return ResponseEntity.ok(proformaInvoiceService.getProformaInvoiceById(id));
    }

    @GetMapping("/pdf/{id}")
    public ResponseEntity<byte[]> generatePdf(@PathVariable Long id) {
        byte[] pdf = proformaInvoiceService.generatePdf(id);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=proforma_invoice_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping
    public ResponseEntity<Page<ProformaInvoiceResponse>> getAllProformaInvoices(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long salespersonId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity
                .ok(proformaInvoiceService.getAllProformaInvoices(search, fromDate, toDate, salespersonId, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProformaInvoice(@PathVariable Long id) {
        proformaInvoiceService.deleteProformaInvoice(id);
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/{id}/status", method = { RequestMethod.PATCH, RequestMethod.POST, RequestMethod.PUT })
    public ResponseEntity<ProformaInvoiceResponse> updateStatus(@PathVariable Long id, @RequestParam SalesStatus status) {
        return ResponseEntity.ok(proformaInvoiceService.updateStatus(id, status));
    }

    // @RequestMapping(value = { "/status-by-number", "/status/by-number" }, method = { RequestMethod.PATCH,
    //         RequestMethod.POST,
    //         RequestMethod.PUT })
    // public ResponseEntity<ProformaInvoiceResponse> updateStatusByNumber(
    //         @RequestParam String invoiceNumber,
    //         @RequestParam SalesStatus status) {
    //     return ResponseEntity.ok(proformaInvoiceService.updateStatusByNumber(invoiceNumber, status));
    // }
}
