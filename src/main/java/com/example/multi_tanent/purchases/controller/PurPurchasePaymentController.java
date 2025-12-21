package com.example.multi_tanent.purchases.controller;

import com.example.multi_tanent.purchases.dto.*;
import com.example.multi_tanent.purchases.service.PurPurchasePaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/purchases/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PurPurchasePaymentController {

    private final PurPurchasePaymentService service;

    @PostMapping
    public ResponseEntity<PurPurchasePaymentResponse> create(
            @ModelAttribute PurPurchasePaymentRequest req,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            jakarta.servlet.http.HttpServletRequest httpReq) {

        System.out.println("DEBUG: create payment called");
        System.out.println("DEBUG: Params: " + httpReq.getParameterMap().keySet());
        try {
            if (httpReq instanceof org.springframework.web.multipart.MultipartHttpServletRequest) {
                System.out.println(
                        "DEBUG: Parts: " + ((org.springframework.web.multipart.MultipartHttpServletRequest) httpReq)
                                .getFileMap().keySet());
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Error checking parts " + e.getMessage());
        }

        // Temporary fallback: if amount is null, maybe it is in a 'request' param as
        // JSON string?

        PurPurchasePaymentResponse resp = service.create(req, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PostMapping("/{id}/attachments")
    public ResponseEntity<List<String>> uploadAttachments(
            @PathVariable Long id,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "uploadedBy", required = false) String uploadedBy) {
        List<String> urls = service.attachFiles(id, files, uploadedBy);
        return ResponseEntity.ok(urls);
    }

    @GetMapping
    public Page<PurPurchasePaymentResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        Sort s = Sort.by(Sort.Direction.DESC, "createdAt");
        try {
            String[] sp = sort.split(",");
            s = Sort.by(Sort.Direction.fromString(sp[1]), sp[0]);
        } catch (Exception ignored) {
        }
        Pageable p = PageRequest.of(page, size, s);
        return service.list(p);
    }

    @GetMapping("/unpaid-invoices")
    public ResponseEntity<List<PurPurchaseInvoiceResponse>> getUnpaidInvoices(@RequestParam Long supplierId) {
        return ResponseEntity.ok(service.getUnpaidInvoices(supplierId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurPurchasePaymentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PurPurchasePaymentResponse> update(@PathVariable Long id,
            @RequestPart("request") @Valid PurPurchasePaymentRequest req,
            @RequestPart(value = "attachments", required = false) MultipartFile[] attachments) {
        return ResponseEntity.ok(service.update(id, req, attachments));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
