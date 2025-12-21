package com.example.multi_tanent.sales.controller;

import com.example.multi_tanent.sales.dto.RentalQuotationRequest;
import com.example.multi_tanent.sales.dto.RentalQuotationResponse;
import com.example.multi_tanent.sales.enums.QuotationType;
import com.example.multi_tanent.sales.enums.SalesStatus;
import com.example.multi_tanent.sales.service.RentalQuotationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/sales/rental-quotations")
@RequiredArgsConstructor
public class RentalQuotationController {

    private final RentalQuotationService rentalQuotationService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RentalQuotationResponse> createRentalQuotation(
            @RequestPart("quotation") RentalQuotationRequest request,
            @RequestPart(value = "attachments", required = false) MultipartFile[] attachments) {
        return ResponseEntity.ok(rentalQuotationService.createRentalQuotation(request, attachments));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RentalQuotationResponse> updateRentalQuotation(@PathVariable Long id,
            @RequestPart("quotation") RentalQuotationRequest request,
            @RequestPart(value = "attachments", required = false) MultipartFile[] attachments) {
        return ResponseEntity.ok(rentalQuotationService.updateRentalQuotation(id, request, attachments));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RentalQuotationResponse> getRentalQuotationById(@PathVariable Long id) {
        return ResponseEntity.ok(rentalQuotationService.getRentalQuotationById(id));
    }

    @GetMapping
    public ResponseEntity<Page<RentalQuotationResponse>> getAllRentalQuotations(
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) SalesStatus status,
            @RequestParam(required = false) Long salespersonId,
            @RequestParam(required = false) QuotationType type,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(rentalQuotationService.getAllRentalQuotations(customerName, fromDate, toDate, status,
                salespersonId, type, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRentalQuotation(@PathVariable Long id) {
        rentalQuotationService.deleteRentalQuotation(id);
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/{id}/status", method = { RequestMethod.PATCH, RequestMethod.POST, RequestMethod.PUT })
    public ResponseEntity<RentalQuotationResponse> updateStatus(@PathVariable Long id,
            @RequestParam SalesStatus status) {
        return ResponseEntity.ok(rentalQuotationService.updateStatus(id, status));
    }

    @RequestMapping(value = { "/status-by-number", "/status/by-number" }, method = { RequestMethod.PATCH,
            RequestMethod.POST, RequestMethod.PUT })
    public ResponseEntity<RentalQuotationResponse> updateStatusByNumber(
            @RequestParam String quotationNumber,
            @RequestParam SalesStatus status) {
        return ResponseEntity.ok(rentalQuotationService.updateStatusByQuotationNumber(quotationNumber, status));
    }
}
