package com.example.multi_tanent.sales.controller;

import com.example.multi_tanent.sales.dto.DeliveryOrderRequest;
import com.example.multi_tanent.sales.dto.DeliveryOrderResponse;
import com.example.multi_tanent.sales.enums.SalesStatus;
import com.example.multi_tanent.sales.service.DeliveryOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/sales/delivery-orders")
@RequiredArgsConstructor
public class DeliveryOrderController {

    private final DeliveryOrderService service;

    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<DeliveryOrderResponse> createDeliveryOrder(
            @RequestPart("data") DeliveryOrderRequest request,
            @RequestPart(value = "attachments", required = false) MultipartFile[] attachments) {
        return ResponseEntity.ok(service.createDeliveryOrder(request, attachments));
    }

    @PutMapping(value = "/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<DeliveryOrderResponse> updateDeliveryOrder(
            @PathVariable Long id,
            @RequestPart("data") DeliveryOrderRequest request,
            @RequestPart(value = "attachments", required = false) MultipartFile[] attachments) {
        return ResponseEntity.ok(service.updateDeliveryOrder(id, request, attachments));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeliveryOrderResponse> getDeliveryOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getDeliveryOrderById(id));
    }

    @GetMapping
    public ResponseEntity<Page<DeliveryOrderResponse>> getAllDeliveryOrders(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate toDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate,
            @RequestParam(required = false) Long salespersonId,
            @PageableDefault(size = 10) Pageable pageable) {

        // Handle legacy parameters for backward compatibility
        String effectiveSearch = (search != null) ? search : customerName;
        java.time.LocalDate effectiveFromDate = (fromDate != null) ? fromDate : startDate;
        java.time.LocalDate effectiveToDate = (toDate != null) ? toDate : endDate;

        return ResponseEntity.ok(
                service.getAllDeliveryOrders(effectiveSearch, effectiveFromDate, effectiveToDate, salespersonId,
                        pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDeliveryOrder(@PathVariable Long id) {
        service.deleteDeliveryOrder(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<DeliveryOrderResponse> updateStatus(@PathVariable Long id, @RequestParam SalesStatus status) {
        return ResponseEntity.ok(service.updateStatus(id, status));
    }
}
