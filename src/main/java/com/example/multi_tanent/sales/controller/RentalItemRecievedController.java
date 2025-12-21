package com.example.multi_tanent.sales.controller;

import com.example.multi_tanent.sales.dto.RentalItemRecievedRequest;
import com.example.multi_tanent.sales.dto.RentalItemRecievedResponse;
import com.example.multi_tanent.sales.enums.SalesStatus;
import com.example.multi_tanent.sales.service.RentalItemRecievedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales/rental-item-received")
@RequiredArgsConstructor
public class RentalItemRecievedController {

    private final RentalItemRecievedService service;

    @PostMapping
    public ResponseEntity<RentalItemRecievedResponse> create(
            @RequestBody RentalItemRecievedRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RentalItemRecievedResponse> update(
            @PathVariable Long id,
            @RequestBody RentalItemRecievedRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id, @RequestParam SalesStatus status) {
        service.updateStatus(id, status);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RentalItemRecievedResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<RentalItemRecievedResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/by-order/{orderId}")
    public ResponseEntity<List<RentalItemRecievedResponse>> getByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(service.getByOrderId(orderId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
