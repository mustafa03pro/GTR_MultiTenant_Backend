package com.example.multi_tanent.sales.controller;

import com.example.multi_tanent.sales.dto.FollowUpRequest;
import com.example.multi_tanent.sales.dto.FollowUpResponse;
import com.example.multi_tanent.sales.service.FollowUpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales/followups")
@RequiredArgsConstructor
public class FollowUpController {

    private final FollowUpService service;

    @PostMapping
    public ResponseEntity<FollowUpResponse> create(@RequestBody FollowUpRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FollowUpResponse> update(
            @PathVariable Long id,
            @RequestBody FollowUpRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FollowUpResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<FollowUpResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/by-quotation/{quotationId}")
    public ResponseEntity<List<FollowUpResponse>> getByQuotationId(@PathVariable Long quotationId) {
        return ResponseEntity.ok(service.getByQuotationId(quotationId));
    }

    @GetMapping("/by-rental-quotation/{rentalQuotationId}")
    public ResponseEntity<List<FollowUpResponse>> getByRentalQuotationId(@PathVariable Long rentalQuotationId) {
        return ResponseEntity.ok(service.getByRentalQuotationId(rentalQuotationId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
