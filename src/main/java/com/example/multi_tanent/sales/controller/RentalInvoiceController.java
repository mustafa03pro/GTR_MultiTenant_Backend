package com.example.multi_tanent.sales.controller;

import com.example.multi_tanent.sales.dto.RentalInvoiceRequest;
import com.example.multi_tanent.sales.dto.RentalInvoiceResponse;
import com.example.multi_tanent.sales.service.RentalInvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/sales/rental-invoices")
@RequiredArgsConstructor
public class RentalInvoiceController {

    private final RentalInvoiceService rentalInvoiceService;

    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RentalInvoiceResponse> createRentalInvoice(
            @RequestPart("rentalInvoice") RentalInvoiceRequest request,
            @RequestPart(value = "attachments", required = false) MultipartFile[] attachments) {
        return ResponseEntity.ok(rentalInvoiceService.createRentalInvoice(request, attachments));
    }

    @PutMapping(value = "/{id}", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RentalInvoiceResponse> updateRentalInvoice(@PathVariable Long id,
            @RequestPart("rentalInvoice") RentalInvoiceRequest request,
            @RequestPart(value = "attachments", required = false) MultipartFile[] attachments) {
        return ResponseEntity.ok(rentalInvoiceService.updateRentalInvoice(id, request, attachments));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RentalInvoiceResponse> getRentalInvoiceById(@PathVariable Long id) {
        return ResponseEntity.ok(rentalInvoiceService.getRentalInvoiceById(id));
    }

    @GetMapping
    public ResponseEntity<Page<RentalInvoiceResponse>> getAllRentalInvoices(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate toDate,
            @RequestParam(required = false) Long salespersonId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity
                .ok(rentalInvoiceService.getAllRentalInvoices(search, fromDate, toDate, salespersonId, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRentalInvoice(@PathVariable Long id) {
        rentalInvoiceService.deleteRentalInvoice(id);
        return ResponseEntity.noContent().build();
    }
}
