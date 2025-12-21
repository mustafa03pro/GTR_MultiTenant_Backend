
package com.example.multi_tanent.purchases.controller;

import com.example.multi_tanent.purchases.dto.*;
import com.example.multi_tanent.purchases.service.PurPurchaseInvoiceService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchases/invoices")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PurPurchaseInvoiceController {

    private final PurPurchaseInvoiceService service;

    @PostMapping(consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<PurPurchaseInvoiceResponse> create(
            @RequestPart("request") @Valid PurPurchaseInvoiceRequest req,
            @RequestPart(value = "attachments", required = false) MultipartFile[] attachments) {
        PurPurchaseInvoiceResponse resp = service.create(req, attachments);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping
    public ResponseEntity<Page<PurPurchaseInvoiceResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        Sort s = Sort.by(Sort.Direction.DESC, "createdAt");
        try {
            String[] sp = sort.split(",", 2);
            if (sp.length == 2) {
                s = Sort.by(Sort.Direction.fromString(sp[1]), sp[0]);
            }
        } catch (Exception ignored) {
        }
        Pageable p = PageRequest.of(page, size, s);
        return ResponseEntity.ok(service.list(p));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurPurchaseInvoiceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PurPurchaseInvoiceResponse> update(@PathVariable Long id,
            @RequestPart("request") @Valid PurPurchaseInvoiceRequest req,
            @RequestPart(value = "attachments", required = false) MultipartFile[] attachments) {
        return ResponseEntity.ok(service.update(id, req, attachments));
    }

    @PostMapping("/{id}/convert-to-payment")
    public ResponseEntity<com.example.multi_tanent.purchases.dto.PurPurchasePaymentResponse> convertToPayment(
            @PathVariable Long id) {
        return ResponseEntity.ok(service.convertToPayment(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
