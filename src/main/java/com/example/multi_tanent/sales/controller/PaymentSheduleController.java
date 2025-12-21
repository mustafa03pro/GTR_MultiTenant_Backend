package com.example.multi_tanent.sales.controller;

import com.example.multi_tanent.sales.dto.PaymentSheduleRequest;
import com.example.multi_tanent.sales.dto.PaymentSheduleResponse;
import com.example.multi_tanent.sales.service.PaymentSheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/sales/payment-schedules")
@RequiredArgsConstructor
public class PaymentSheduleController {

    private final PaymentSheduleService paymentSheduleService;

    @PostMapping
    public ResponseEntity<PaymentSheduleResponse> createPaymentShedule(@RequestBody PaymentSheduleRequest request) {
        return ResponseEntity.ok(paymentSheduleService.createPaymentShedule(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentSheduleResponse> updatePaymentShedule(@PathVariable Long id,
            @RequestBody PaymentSheduleRequest request) {
        return ResponseEntity.ok(paymentSheduleService.updatePaymentShedule(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentSheduleResponse> getPaymentSheduleById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentSheduleService.getPaymentSheduleById(id));
    }

    @GetMapping
    public ResponseEntity<Page<PaymentSheduleResponse>> getAllPaymentSchedules(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long rentalSalesOrderId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(paymentSheduleService.getAllPaymentSchedules(search, fromDate, toDate, customerId,
                rentalSalesOrderId, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePaymentShedule(@PathVariable Long id) {
        paymentSheduleService.deletePaymentShedule(id);
        return ResponseEntity.noContent().build();
    }
}
