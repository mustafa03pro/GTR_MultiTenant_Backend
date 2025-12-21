package com.example.multi_tanent.sales.controller;

import com.example.multi_tanent.sales.dto.SalesInvoiceRequest;
import com.example.multi_tanent.sales.dto.SalesInvoiceResponse;
import com.example.multi_tanent.sales.service.SalesInvoiceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/sales/sales-invoices")
@RequiredArgsConstructor
public class SalesInvoiceController {

    private final SalesInvoiceService salesInvoiceService;
    private final ObjectMapper objectMapper;

    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<SalesInvoiceResponse> createSalesInvoice(
            @RequestPart("data") String data,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) throws JsonProcessingException {
        SalesInvoiceRequest request = objectMapper.readValue(data, SalesInvoiceRequest.class);
        return ResponseEntity.ok(salesInvoiceService.createSalesInvoice(request, files));
    }

    @PutMapping(value = "/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<SalesInvoiceResponse> updateSalesInvoice(
            @PathVariable Long id,
            @RequestPart("data") String data,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) throws JsonProcessingException {
        SalesInvoiceRequest request = objectMapper.readValue(data, SalesInvoiceRequest.class);
        return ResponseEntity.ok(salesInvoiceService.updateSalesInvoice(id, request, files));
    }

    @PutMapping(value = "/{id}", consumes = { MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<SalesInvoiceResponse> updateSalesInvoiceJson(
            @PathVariable Long id,
            @RequestBody SalesInvoiceRequest request) {
        return ResponseEntity.ok(salesInvoiceService.updateSalesInvoice(id, request, null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalesInvoiceResponse> getSalesInvoiceById(@PathVariable Long id) {
        return ResponseEntity.ok(salesInvoiceService.getSalesInvoiceById(id));
    }

    @GetMapping
    public ResponseEntity<Page<SalesInvoiceResponse>> getAllSalesInvoices(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long salespersonId,
            Pageable pageable) {
        return ResponseEntity
                .ok(salesInvoiceService.getAllSalesInvoices(search, fromDate, toDate, salespersonId, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSalesInvoice(@PathVariable Long id) {
        salesInvoiceService.deleteSalesInvoice(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pdf/{id}")
    public ResponseEntity<byte[]> generatePdf(@PathVariable Long id) {
        byte[] pdf = salesInvoiceService.generatePdf(id);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=sales_invoice_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
