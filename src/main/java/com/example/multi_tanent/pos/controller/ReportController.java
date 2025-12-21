package com.example.multi_tanent.pos.controller;

import com.example.multi_tanent.pos.dto.report.BusinessSummaryDto;
import com.example.multi_tanent.pos.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.io.ByteArrayInputStream;
import java.util.List;

@RestController
@RequestMapping("/api/pos/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/business-summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'POS_ADMIN')")
    public ResponseEntity<BusinessSummaryDto> getBusinessSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(reportService.getBusinessSummary(fromDate, toDate));
    }

    @GetMapping("/business-summary/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'POS_ADMIN')")
    public ResponseEntity<InputStreamResource> exportBusinessSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        ByteArrayInputStream in = reportService.exportBusinessSummaryToExcel(fromDate, toDate);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=business_summary.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }

    @GetMapping("/daily-sales")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'POS_ADMIN')")
    public ResponseEntity<com.example.multi_tanent.pos.dto.report.DailySalesSummaryDto> getDailySalesSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(reportService.getDailySalesSummary(date));
    }

    @GetMapping("/sales-by-hour")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'POS_ADMIN')")
    public ResponseEntity<List<com.example.multi_tanent.pos.dto.report.SalesByHourDto>> getSalesByHour(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(reportService.getSalesByHour(fromDate, toDate));
    }

    @GetMapping("/closing-reports")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'POS_ADMIN')")
    public ResponseEntity<List<com.example.multi_tanent.pos.dto.report.ClosingReportDto>> getClosingReports(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(reportService.getClosingReports(fromDate, toDate));
    }

    @GetMapping("/sales-support-report")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'POS_ADMIN')")
    public ResponseEntity<List<com.example.multi_tanent.pos.dto.report.SalesSupportReportItemDto>> getSalesSupportReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long storeId) {
        return ResponseEntity.ok(reportService.getSalesSupportReport(fromDate, toDate, storeId));
    }

    @GetMapping("/sales-payment-report")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'POS_ADMIN')")
    public ResponseEntity<List<com.example.multi_tanent.pos.dto.report.SalesPaymentReportItemDto>> getSalesPaymentReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String paymentMethod) {
        return ResponseEntity.ok(reportService.getSalesPaymentReport(fromDate, toDate, storeId, paymentMethod));
    }

    @PostMapping("/user-transfer")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'POS_ADMIN')")
    public ResponseEntity<String> transferUserActivity(
            @RequestBody com.example.multi_tanent.pos.dto.report.UserTransferRequestDto request) {
        reportService.transferUserActivity(request);
        return ResponseEntity.ok("User activity transferred successfully.");
    }

    @GetMapping("/sales-source-report")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'POS_ADMIN')")
    public ResponseEntity<List<com.example.multi_tanent.pos.dto.report.SalesSourceReportItemDto>> getSalesSourceReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String reference) {
        return ResponseEntity.ok(reportService.getSalesSourceReport(fromDate, toDate, storeId, source, reference));
    }

    @GetMapping("/total-sales-report")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'POS_ADMIN')")
    public ResponseEntity<List<com.example.multi_tanent.pos.dto.report.TotalSalesReportItemDto>> getTotalSalesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long storeId) {
        return ResponseEntity.ok(reportService.getTotalSalesReport(fromDate, toDate, storeId));
    }

    @GetMapping("/sales-status-report")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'POS_ADMIN')")
    public ResponseEntity<Object> getSalesStatusReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(reportService.getSalesStatusReport(fromDate, toDate, status));
    }

    @GetMapping("/delivery-driver-report")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'POS_ADMIN')")
    public ResponseEntity<List<com.example.multi_tanent.pos.dto.report.DeliveryDriverReportItemDto>> getDeliveryDriverReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long driverId) {
        return ResponseEntity.ok(reportService.getDeliveryDriverReport(fromDate, toDate, driverId));
    }

    @GetMapping("/sales-forecasting-report")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'POS_ADMIN')")
    public ResponseEntity<List<com.example.multi_tanent.pos.dto.report.SalesForecastingReportItemDto>> getSalesForecastingReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long storeId) {
        return ResponseEntity.ok(reportService.getSalesForecastingReport(fromDate, toDate, storeId));
    }

    @GetMapping("/sales-discount-report")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'POS_ADMIN')")
    public ResponseEntity<List<com.example.multi_tanent.pos.dto.report.SalesDiscountReportItemDto>> getSalesDiscountReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String discountType) {
        return ResponseEntity.ok(reportService.getSalesDiscountReport(fromDate, toDate, storeId, discountType));
    }

    @GetMapping("/cancellation-report")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'POS_ADMIN')")
    public ResponseEntity<List<com.example.multi_tanent.pos.dto.report.CancellationReportItemDto>> getCancellationReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long storeId) {
        return ResponseEntity.ok(reportService.getCancellationReport(fromDate, toDate, storeId));
    }

    @GetMapping("/void-report")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'POS_ADMIN')")
    public ResponseEntity<List<com.example.multi_tanent.pos.dto.report.VoidReportItemDto>> getVoidReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long storeId) {
        return ResponseEntity.ok(reportService.getVoidReport(fromDate, toDate, storeId));
    }

    @GetMapping("/item-movement-report")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'POS_ADMIN')")
    public ResponseEntity<List<com.example.multi_tanent.pos.dto.report.ItemMovementReportItemDto>> getItemMovementReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String itemName) {
        return ResponseEntity.ok(reportService.getItemMovementReport(fromDate, toDate, storeId, itemName));
    }

    @GetMapping("/slow-selling-report")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'POS_ADMIN')")
    public ResponseEntity<List<com.example.multi_tanent.pos.dto.report.SlowSellingReportItemDto>> getSlowSellingReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false, defaultValue = "By Quantity") String orderType) {
        return ResponseEntity.ok(reportService.getSlowSellingReport(fromDate, toDate, storeId, orderType));
    }

    @GetMapping("/best-selling-report")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'POS_ADMIN')")
    public ResponseEntity<List<com.example.multi_tanent.pos.dto.report.BestSellingReportItemDto>> getBestSellingReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false, defaultValue = "By Quantity") String orderType) {
        return ResponseEntity.ok(reportService.getBestSellingReport(fromDate, toDate, storeId, orderType));
    }

    @GetMapping("/item-sales-report")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'POS_ADMIN')")
    public ResponseEntity<List<com.example.multi_tanent.pos.dto.report.ItemSalesReportItemDto>> getItemSalesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String itemName) {
        return ResponseEntity.ok(reportService.getItemSalesReport(fromDate, toDate, storeId, categoryId, itemName));
    }
}
