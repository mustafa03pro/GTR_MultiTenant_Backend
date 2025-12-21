package com.example.multi_tanent.sales.controller;

import com.example.multi_tanent.sales.dto.SalesOrderRequest;
import com.example.multi_tanent.sales.dto.SalesOrderResponse;
import com.example.multi_tanent.sales.enums.SalesStatus;
import com.example.multi_tanent.sales.service.SalesOrderPdfService;
import com.example.multi_tanent.sales.service.SalesOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales/orders")
@RequiredArgsConstructor
public class SalesOrderController {

    private final SalesOrderService salesOrderService;
    private final SalesOrderPdfService salesOrderPdfService;

    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SalesOrderResponse> createSalesOrder(
            @RequestPart("salesOrder") SalesOrderRequest request,
            @RequestPart(value = "attachments", required = false) org.springframework.web.multipart.MultipartFile[] attachments) {
        return ResponseEntity.ok(salesOrderService.createSalesOrder(request, attachments));
    }

    @PutMapping(value = "/{id}", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SalesOrderResponse> updateSalesOrder(@PathVariable Long id,
            @RequestPart("salesOrder") SalesOrderRequest request,
            @RequestPart(value = "attachments", required = false) org.springframework.web.multipart.MultipartFile[] attachments) {
        return ResponseEntity.ok(salesOrderService.updateSalesOrder(id, request, attachments));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalesOrderResponse> getSalesOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(salesOrderService.getSalesOrderById(id));
    }

    @GetMapping
    public ResponseEntity<Page<SalesOrderResponse>> getAllSalesOrders(
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long salespersonId,
            @PageableDefault(size = 10) Pageable pageable) {

        SalesStatus statusEnum = null;
        if (status != null && !status.isEmpty() && !"All".equalsIgnoreCase(status)) {
            try {
                statusEnum = SalesStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // ignore invalid status
            }
        }

        return ResponseEntity.ok(salesOrderService.getAllSalesOrders(customerName, startDate, endDate, statusEnum,
                salespersonId, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSalesOrder(@PathVariable Long id) {
        salesOrderService.deleteSalesOrder(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SalesOrderResponse> updateStatus(@PathVariable Long id, @RequestParam SalesStatus status) {
        return ResponseEntity.ok(salesOrderService.updateStatus(id, status));
    }

    @PatchMapping("/update-status-by-number")
    public ResponseEntity<SalesOrderResponse> updateStatusByNumber(
            @RequestParam String salesOrderNumber,
            @RequestParam SalesStatus status) {
        return ResponseEntity.ok(salesOrderService.updateStatusByNumber(salesOrderNumber, status));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        SalesOrderResponse salesOrder = salesOrderService.getSalesOrderById(id);
        byte[] pdfBytes = salesOrderPdfService.generateSalesOrderPdf(salesOrder);

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=sales_order_" + salesOrder.getSalesOrderNumber() + ".pdf")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @PostMapping("/generate-pdf")
    public ResponseEntity<byte[]> generatePdfFromHtml(
            @RequestBody com.example.multi_tanent.sales.dto.PdfGenerationRequest request) {
        byte[] pdfBytes = salesOrderPdfService.generatePdfFromHtml(request.getHtmlContent());
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=generated_invoice.pdf")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @PostMapping("/from-quotation/{quotationId}")
    public ResponseEntity<SalesOrderResponse> createSalesOrderFromQuotation(@PathVariable Long quotationId) {
        return ResponseEntity.ok(salesOrderService.createSalesOrderFromQuotation(quotationId));
    }
}
