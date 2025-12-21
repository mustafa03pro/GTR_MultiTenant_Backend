package com.example.multi_tanent.sales.controller;

import com.example.multi_tanent.sales.dto.RentalSalesOrderRequest;
import com.example.multi_tanent.sales.dto.RentalSalesOrderResponse;
import com.example.multi_tanent.sales.service.RentalSalesOrderService;
import com.example.multi_tanent.sales.enums.SalesStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/sales/rental-sales-orders")
@RequiredArgsConstructor
public class RentalSalesOrderController {

    private final RentalSalesOrderService rentalSalesOrderService;

    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RentalSalesOrderResponse> createRentalSalesOrder(
            @RequestPart("order") RentalSalesOrderRequest request,
            @RequestPart(value = "attachments", required = false) MultipartFile[] attachments) {
        return ResponseEntity.ok(rentalSalesOrderService.createRentalSalesOrder(request, attachments));
    }

    @PutMapping(value = "/{id}", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RentalSalesOrderResponse> updateRentalSalesOrder(@PathVariable Long id,
            @RequestPart("order") RentalSalesOrderRequest request,
            @RequestPart(value = "attachments", required = false) MultipartFile[] attachments) {
        return ResponseEntity.ok(rentalSalesOrderService.updateRentalSalesOrder(id, request, attachments));
    }

    // Not explicitly in service yet, but standard pattern

    @GetMapping("/{id}")
    public ResponseEntity<RentalSalesOrderResponse> getRentalSalesOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(rentalSalesOrderService.getRentalSalesOrderById(id));
    }

    @GetMapping
    public ResponseEntity<Page<RentalSalesOrderResponse>> getAllRentalSalesOrders(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate toDate,
            @RequestParam(required = false) Long salespersonId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity
                .ok(rentalSalesOrderService.getAllRentalSalesOrders(search, fromDate, toDate, salespersonId, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRentalSalesOrder(@PathVariable Long id) {
        rentalSalesOrderService.deleteRentalSalesOrder(id);
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/{id}/status", method = { RequestMethod.PATCH, RequestMethod.POST, RequestMethod.PUT })
    public ResponseEntity<RentalSalesOrderResponse> updateStatus(@PathVariable Long id,
            @RequestParam SalesStatus status) {
        return ResponseEntity.ok(rentalSalesOrderService.updateStatus(id, status));
    }

}
