package com.example.multi_tanent.pos.service;

import com.example.multi_tanent.pos.dto.report.BusinessSummaryDto;
import com.example.multi_tanent.pos.entity.Sale;
import com.example.multi_tanent.pos.repository.SaleRepository;
import com.example.multi_tanent.spersusers.repository.TenantRepository;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service; // Removed unused import to fix warning

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@Transactional("tenantTx")
public class ReportService {

        private final SaleRepository saleRepository;
        private final TenantRepository tenantRepository;
        private final com.example.multi_tanent.pos.repository.StockMovementRepository stockMovementRepository;
        private final com.example.multi_tanent.pos.repository.PaymentRepository paymentRepository;
        private final com.example.multi_tanent.spersusers.repository.StoreRepository storeRepository;
        private final com.example.multi_tanent.pos.repository.SalesForecastRepository salesForecastRepository;
        private final com.example.multi_tanent.pos.repository.PaymentChangeLogRepository paymentChangeLogRepository;

        public ReportService(SaleRepository saleRepository, TenantRepository tenantRepository,
                        com.example.multi_tanent.pos.repository.StockMovementRepository stockMovementRepository,
                        com.example.multi_tanent.pos.repository.PaymentRepository paymentRepository,
                        com.example.multi_tanent.spersusers.repository.StoreRepository storeRepository,
                        com.example.multi_tanent.pos.repository.SalesForecastRepository salesForecastRepository,
                        com.example.multi_tanent.pos.repository.PaymentChangeLogRepository paymentChangeLogRepository) {
                this.saleRepository = saleRepository;
                this.tenantRepository = tenantRepository;
                this.stockMovementRepository = stockMovementRepository;
                this.paymentRepository = paymentRepository;
                this.storeRepository = storeRepository;
                this.salesForecastRepository = salesForecastRepository;
                this.paymentChangeLogRepository = paymentChangeLogRepository;
        }

        private Tenant getCurrentTenant() {
                return tenantRepository.findFirstByOrderByIdAsc()
                                .orElseThrow(() -> new IllegalStateException("Tenant context not found."));
        }

        public BusinessSummaryDto getBusinessSummary(LocalDate fromDate, LocalDate toDate) {
                Tenant tenant = getCurrentTenant();

                OffsetDateTime start = fromDate.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime();
                OffsetDateTime end = toDate.plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime()
                                .minusNanos(1);

                List<Sale> sales = saleRepository.findByTenantIdAndInvoiceDateBetween(tenant.getId(), start, end);

                // 1. Sales General Summary
                long totalSubtotalCents = 0;
                long totalDelivery = 0;
                long totalDiscount = 0;
                long totalTax = 0;
                long totalGross = 0;

                for (Sale sale : sales) {
                        if ("completed".equalsIgnoreCase(sale.getStatus())
                                        || "paid".equalsIgnoreCase(sale.getPaymentStatus())) {
                                totalSubtotalCents += sale.getSubtotalCents();
                                totalDelivery += (sale.getDeliveryCharge() != null ? sale.getDeliveryCharge() : 0);
                                totalDiscount += (sale.getDiscountCents() != null ? sale.getDiscountCents() : 0);
                                totalTax += sale.getTaxCents();
                                totalGross += sale.getTotalCents(); // Total is Sub + Tax - Disc + Delivery presumably,
                                                                    // but checking
                                                                    // logic: Total was calculated as sub + tax - disc.
                                                                    // Delivery might
                                                                    // be missing in calculation?
                                // Checking SaleService logic: sale.setTotalCents(subtotal + totalTax -
                                // sale.getDiscountCents());
                                // It seems Delivery Charge was NOT included in Total Logic in SaleService?
                                // Let's assume for report we aggregate fields as they are.
                        }
                }

                // Re-calculating report metrics based on cents
                BigDecimal salesVal = toBigDecimal(totalSubtotalCents);
                BigDecimal deliveryVal = toBigDecimal(totalDelivery);
                BigDecimal discountVal = toBigDecimal(totalDiscount);
                BigDecimal vatVal = toBigDecimal(totalTax);

                BigDecimal grossSalesVal = salesVal.add(deliveryVal); // As per typical summary.
                BigDecimal netSalesIncVat = grossSalesVal.subtract(discountVal);
                BigDecimal netSalesExVat = netSalesIncVat.subtract(vatVal);

                BusinessSummaryDto.SalesSummary summary = new BusinessSummaryDto.SalesSummary(
                                salesVal,
                                deliveryVal,
                                BigDecimal.ZERO, // Paid Modifiers
                                grossSalesVal,
                                discountVal,
                                netSalesIncVat,
                                vatVal,
                                netSalesExVat,
                                netSalesExVat // FnB sales minus VAT
                );

                // 2. Order Types
                Map<String, List<Sale>> byOrderType = sales.stream()
                                .filter(s -> s.getOrderType() != null)
                                .collect(Collectors.groupingBy(s -> s.getOrderType().name()));

                List<BusinessSummaryDto.OrderTypeSummary> orderTypes = new ArrayList<>();
                orderTypes.add(createOrderTypeSummary("Dine In", byOrderType.get("DINE_IN")));
                orderTypes.add(createOrderTypeSummary("Takeaway", byOrderType.get("TAKEAWAY")));
                orderTypes.add(createOrderTypeSummary("Delivery", byOrderType.get("DELIVERY")));

                // Calculate Totals for Order Types
                long totalOrders = orderTypes.stream().mapToLong(BusinessSummaryDto.OrderTypeSummary::getOrdersCount)
                                .sum();
                BigDecimal totalOrderValue = orderTypes.stream().map(BusinessSummaryDto.OrderTypeSummary::getValue)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                orderTypes.add(new BusinessSummaryDto.OrderTypeSummary("Total", totalOrders, totalOrderValue));

                // 3. Sales Source
                Map<String, List<Sale>> bySource = sales.stream()
                                .collect(Collectors.groupingBy(
                                                s -> s.getSalesSource() != null ? s.getSalesSource() : "Unknown"));

                List<BusinessSummaryDto.SalesSourceSummary> sources = new ArrayList<>();
                BigDecimal totalSourceAmount = BigDecimal.ZERO;
                long totalSourceQty = 0;

                for (Map.Entry<String, List<Sale>> entry : bySource.entrySet()) {
                        BigDecimal amt = entry.getValue().stream()
                                        .map(s -> toBigDecimal(s.getTotalCents()))
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                        long qty = entry.getValue().size();
                        sources.add(new BusinessSummaryDto.SalesSourceSummary(entry.getKey(), qty, amt));

                        totalSourceAmount = totalSourceAmount.add(amt);
                        totalSourceQty += qty;
                }
                sources.add(new BusinessSummaryDto.SalesSourceSummary("Total", totalSourceQty, totalSourceAmount));

                // 4. Guest Count
                long adults = sales.stream().mapToLong(s -> s.getAdultsCount() != null ? s.getAdultsCount() : 0).sum();
                long kids = sales.stream().mapToLong(s -> s.getKidsCount() != null ? s.getKidsCount() : 0).sum();

                List<BusinessSummaryDto.GuestCountSummary> guests = new ArrayList<>();
                guests.add(new BusinessSummaryDto.GuestCountSummary("Adults Count", adults));
                guests.add(new BusinessSummaryDto.GuestCountSummary("Kids Count", kids));
                guests.add(new BusinessSummaryDto.GuestCountSummary("Total", adults + kids));

                // 5. Cost & Profit & Wastage
                // COGS = Sum of (SaleItem.costCents * quantity)
                long totalCogsCents = 0;
                for (Sale sale : sales) {
                        if ("completed".equalsIgnoreCase(sale.getStatus())
                                        || "paid".equalsIgnoreCase(sale.getPaymentStatus())) {
                                for (com.example.multi_tanent.pos.entity.SaleItem item : sale.getItems()) {
                                        long cost = item.getCostCents() != null ? item.getCostCents() : 0;
                                        // item.getQuantity() is already multiplied in line totals usually, but cost is
                                        // per unit?
                                        // Checking SaleService: item.setCostCents(variant.getCostCents()) -> Unit cost.
                                        totalCogsCents += (cost * item.getQuantity());
                                }
                        }
                }

                // Wastage from Stock Movements
                List<com.example.multi_tanent.pos.entity.StockMovement> wastageMovements = stockMovementRepository
                                .findByTenantIdAndReasonContainingIgnoreCaseAndCreatedAtBetween(tenant.getId(),
                                                "Wastage", start, end);

                long totalWastageCents = 0;
                for (com.example.multi_tanent.pos.entity.StockMovement sm : wastageMovements) {
                        // Wastage is usually a negative changeQuantity. We want the positive cost
                        // value.
                        long qty = Math.abs(sm.getChangeQuantity());
                        // Need cost. StockMovement doesn't store cost snapshot?
                        // It links to ProductVariant. We use current cost if snapshot missing.
                        long unitCost = sm.getProductVariant().getCostCents();
                        totalWastageCents += (qty * unitCost);
                }

                BigDecimal cogsVal = toBigDecimal(totalCogsCents);
                BigDecimal wastageVal = toBigDecimal(totalWastageCents);
                // Gross Profit = Net Sales (ex VAT) - COGS - Wastage (Optional: usually GP
                // don't include wastage, but user might want it)
                // Let's definition: Gross Profit = Net Sales (Ex VAT) - COGS.
                BigDecimal grossProfitVal = netSalesExVat.subtract(cogsVal);

                BusinessSummaryDto.CostProfitSummary costProfit = new BusinessSummaryDto.CostProfitSummary(cogsVal,
                                wastageVal,
                                grossProfitVal);
                BusinessSummaryDto.WastageSummary wastageSummary = new BusinessSummaryDto.WastageSummary(wastageVal);

                // 6. Collections (Payment Methods)
                Map<String, List<com.example.multi_tanent.pos.entity.Payment>> paymentsByMethod = sales.stream()
                                .filter(s -> ("completed".equalsIgnoreCase(s.getStatus())
                                                || "paid".equalsIgnoreCase(s.getPaymentStatus()))
                                                && s.getPayments() != null)
                                .flatMap(s -> s.getPayments().stream())
                                .collect(Collectors.groupingBy(p -> p.getMethod() != null ? p.getMethod() : "Unknown"));

                List<BusinessSummaryDto.CollectionsSummary> collections = new ArrayList<>();
                BigDecimal totalCollection = BigDecimal.ZERO;
                for (Map.Entry<String, List<com.example.multi_tanent.pos.entity.Payment>> entry : paymentsByMethod
                                .entrySet()) {
                        BigDecimal amt = entry.getValue().stream().map(p -> toBigDecimal(p.getAmountCents()))
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                        collections.add(
                                        new BusinessSummaryDto.CollectionsSummary(entry.getKey(),
                                                        (long) entry.getValue().size(), amt));
                        totalCollection = totalCollection.add(amt);
                }
                // Add total row
                // collections.add(new BusinessSummaryDto.CollectionsSummary("Total Collection",
                // 0L, totalCollection)); // Optional

                // 7. Tax Report
                // Group SaleItems by TaxRate
                // Map<RateName, List<SaleItem>>
                Map<String, List<com.example.multi_tanent.pos.entity.SaleItem>> itemsByTax = sales.stream()
                                .filter(s -> "completed".equalsIgnoreCase(s.getStatus())
                                                || "paid".equalsIgnoreCase(s.getPaymentStatus()))
                                .flatMap(s -> s.getItems().stream())
                                .collect(Collectors.groupingBy(i -> {
                                        if (i.getProductVariant() != null
                                                        && i.getProductVariant().getTaxRate() != null) {
                                                return i.getProductVariant().getTaxRate().getName() + " ("
                                                                + i.getProductVariant().getTaxRate().getPercent()
                                                                + "%)";
                                        }
                                        return "No Tax";
                                }));

                List<BusinessSummaryDto.TaxReportSummary> taxReports = new ArrayList<>();
                for (Map.Entry<String, List<com.example.multi_tanent.pos.entity.SaleItem>> entry : itemsByTax
                                .entrySet()) {
                        BigDecimal salesAmt = entry.getValue().stream().map(i -> toBigDecimal(i.getLineTotalCents()))
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal taxAmt = entry.getValue().stream().map(i -> toBigDecimal(i.getTaxCents()))
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                        // Percent is tricky to parse back from string, assuming user just wants display
                        // name.
                        // We can fetch percent from first item.
                        BigDecimal percent = BigDecimal.ZERO;
                        if (!entry.getValue().isEmpty()) {
                                com.example.multi_tanent.pos.entity.SaleItem first = entry.getValue().get(0);
                                if (first.getProductVariant() != null
                                                && first.getProductVariant().getTaxRate() != null) {
                                        percent = first.getProductVariant().getTaxRate().getPercent();
                                }
                        }
                        taxReports.add(new BusinessSummaryDto.TaxReportSummary(entry.getKey(), percent, salesAmt,
                                        taxAmt));
                }

                // 8. Staff Report
                Map<String, List<Sale>> byStaff = sales.stream()
                                .filter(s -> ("completed".equalsIgnoreCase(s.getStatus())
                                                || "paid".equalsIgnoreCase(s.getPaymentStatus()))
                                                && s.getUser() != null)
                                .collect(Collectors.groupingBy(
                                                s -> s.getUser().getName() != null ? s.getUser().getName()
                                                                : s.getUser().getEmail())); // Fallback
                                                                                            // to
                                                                                            // email

                List<BusinessSummaryDto.StaffReportSummary> staffReports = new ArrayList<>();
                for (Map.Entry<String, List<Sale>> entry : byStaff.entrySet()) {
                        long qty = entry.getValue().size();
                        BigDecimal amt = entry.getValue().stream().map(s -> toBigDecimal(s.getTotalCents())).reduce(
                                        BigDecimal.ZERO,
                                        BigDecimal::add);
                        staffReports.add(new BusinessSummaryDto.StaffReportSummary(entry.getKey(), qty, amt));
                }

                // 9. Category Report
                // Group SaleItems by Category
                Map<String, List<com.example.multi_tanent.pos.entity.SaleItem>> itemsByCategory = sales.stream()
                                .filter(s -> "completed".equalsIgnoreCase(s.getStatus())
                                                || "paid".equalsIgnoreCase(s.getPaymentStatus()))
                                .flatMap(s -> s.getItems().stream())
                                .collect(Collectors.groupingBy(i -> {
                                        if (i.getProductVariant() != null && i.getProductVariant().getProduct() != null
                                                        && i.getProductVariant().getProduct().getCategory() != null) {
                                                return i.getProductVariant().getProduct().getCategory().getName();
                                        }
                                        return "Uncategorized";
                                }));

                List<BusinessSummaryDto.CategoryReportSummary> categoryReports = new ArrayList<>();
                for (Map.Entry<String, List<com.example.multi_tanent.pos.entity.SaleItem>> entry : itemsByCategory
                                .entrySet()) {
                        long qty = entry.getValue().stream()
                                        .mapToLong(com.example.multi_tanent.pos.entity.SaleItem::getQuantity)
                                        .sum();
                        BigDecimal amt = entry.getValue().stream().map(i -> toBigDecimal(i.getLineTotalCents()))
                                        .reduce(BigDecimal.ZERO, BigDecimal::add); // Sales amount for category
                        categoryReports.add(new BusinessSummaryDto.CategoryReportSummary(entry.getKey(), qty, amt));
                }

                return new BusinessSummaryDto(summary, orderTypes, sources, guests, costProfit, collections, taxReports,
                                staffReports, categoryReports, wastageSummary);
        }

        private BusinessSummaryDto.OrderTypeSummary createOrderTypeSummary(String label, List<Sale> typeSales) {
                if (typeSales == null || typeSales.isEmpty()) {
                        return new BusinessSummaryDto.OrderTypeSummary(label, 0L, BigDecimal.ZERO);
                }
                BigDecimal value = typeSales.stream()
                                .map(s -> toBigDecimal(s.getTotalCents())) // Using Total Cents for value
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                return new BusinessSummaryDto.OrderTypeSummary(label, (long) typeSales.size(), value);
        }

        private BigDecimal toBigDecimal(Long cents) {
                if (cents == null)
                        return BigDecimal.ZERO;
                return BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100));
        }

        public ByteArrayInputStream exportBusinessSummaryToExcel(LocalDate fromDate, LocalDate toDate) {
                BusinessSummaryDto data = getBusinessSummary(fromDate, toDate);

                try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                        // Sheet 1: Sales Summary
                        Sheet summarySheet = workbook.createSheet("Sales Summary");
                        Row headerRow = summarySheet.createRow(0);
                        headerRow.createCell(0).setCellValue("Metric");
                        headerRow.createCell(1).setCellValue("Value");

                        int rowIdx = 1;
                        BusinessSummaryDto.SalesSummary s = data.getSalesSummary();
                        addRow(summarySheet, rowIdx++, "Sales", s.getSales());
                        addRow(summarySheet, rowIdx++, "Delivery", s.getDeliveryCharge());
                        addRow(summarySheet, rowIdx++, "Paid Modifiers", s.getPaidModifiers());
                        addRow(summarySheet, rowIdx++, "Gross Sales", s.getGrossSales());
                        addRow(summarySheet, rowIdx++, "Discount", s.getDiscounts());
                        addRow(summarySheet, rowIdx++, "Net Sales (Inc VAT)", s.getNetSalesIncludingVat());
                        addRow(summarySheet, rowIdx++, "VAT", s.getVat());
                        addRow(summarySheet, rowIdx++, "Net Sales (Ex VAT)", s.getNetSalesExcludingVat());

                        // Sheet 2: Order Types
                        Sheet orderTypeSheet = workbook.createSheet("Order Types");
                        headerRow = orderTypeSheet.createRow(0);
                        headerRow.createCell(0).setCellValue("Order Type");
                        headerRow.createCell(1).setCellValue("Orders Count");
                        headerRow.createCell(2).setCellValue("Value");

                        rowIdx = 1;
                        for (BusinessSummaryDto.OrderTypeSummary ot : data.getOrderTypes()) {
                                Row row = orderTypeSheet.createRow(rowIdx++);
                                row.createCell(0).setCellValue(ot.getOrderType());
                                row.createCell(1).setCellValue(ot.getOrdersCount());
                                row.createCell(2).setCellValue(ot.getValue().doubleValue());
                        }

                        // Sheet 3: Sales Source
                        Sheet sourceSheet = workbook.createSheet("Sales Source");
                        headerRow = sourceSheet.createRow(0);
                        headerRow.createCell(0).setCellValue("Source");
                        headerRow.createCell(1).setCellValue("Orders Count");
                        headerRow.createCell(2).setCellValue("Amount");
                        rowIdx = 1;

                        for (BusinessSummaryDto.SalesSourceSummary ss : data.getSalesSources()) {
                                Row row = sourceSheet.createRow(rowIdx++);
                                row.createCell(0).setCellValue(ss.getSalesSource());
                                row.createCell(1).setCellValue(ss.getQuantity());
                                row.createCell(2).setCellValue(ss.getAmount().doubleValue());
                        }

                        // Sheet 4: Guest Count
                        Sheet guestSheet = workbook.createSheet("Guest Count");
                        headerRow = guestSheet.createRow(0);
                        headerRow.createCell(0).setCellValue("Type");
                        headerRow.createCell(1).setCellValue("Count");
                        rowIdx = 1;
                        for (BusinessSummaryDto.GuestCountSummary gc : data.getGuestCounts()) {
                                Row row = guestSheet.createRow(rowIdx++);
                                row.createCell(0).setCellValue(gc.getDescription());
                                row.createCell(1).setCellValue(gc.getCount());
                        }

                        // Sheet 5: Cost & Profit
                        Sheet costSheet = workbook.createSheet("Cost & Profit");
                        headerRow = costSheet.createRow(0);
                        headerRow.createCell(0).setCellValue("Metric");
                        headerRow.createCell(1).setCellValue("Value");
                        rowIdx = 1;
                        BusinessSummaryDto.CostProfitSummary cp = data.getCostProfit();
                        addRow(costSheet, rowIdx++, "COGS", cp.getCogs());
                        addRow(costSheet, rowIdx++, "Wastage", cp.getWastage());
                        addRow(costSheet, rowIdx++, "Gross Profit", cp.getGrossProfit());

                        // Sheet 6: Collections
                        Sheet paymentSheet = workbook.createSheet("Collections");
                        headerRow = paymentSheet.createRow(0);
                        headerRow.createCell(0).setCellValue("Method");
                        headerRow.createCell(1).setCellValue("Transactions");
                        headerRow.createCell(2).setCellValue("Amount");
                        rowIdx = 1;
                        for (BusinessSummaryDto.CollectionsSummary cs : data.getCollections()) {
                                Row row = paymentSheet.createRow(rowIdx++);
                                row.createCell(0).setCellValue(cs.getMethod());
                                row.createCell(1).setCellValue(cs.getQuantity());
                                row.createCell(2).setCellValue(cs.getAmount().doubleValue());
                        }

                        // Sheet 7: Tax
                        Sheet taxSheet = workbook.createSheet("Tax Report");
                        headerRow = taxSheet.createRow(0);
                        headerRow.createCell(0).setCellValue("Tax");
                        headerRow.createCell(1).setCellValue("Rate %");
                        headerRow.createCell(2).setCellValue("Sales Amount");
                        headerRow.createCell(3).setCellValue("Tax Amount");
                        rowIdx = 1;
                        for (BusinessSummaryDto.TaxReportSummary tr : data.getTaxReports()) {
                                Row row = taxSheet.createRow(rowIdx++);
                                row.createCell(0).setCellValue(tr.getRateName());
                                row.createCell(1).setCellValue(tr.getRatePercent().doubleValue());
                                row.createCell(2).setCellValue(tr.getSalesAmount().doubleValue());
                                row.createCell(3).setCellValue(tr.getTaxAmount().doubleValue());
                        }

                        // Sheet 8: Staff
                        Sheet staffSheet = workbook.createSheet("Staff Report");
                        headerRow = staffSheet.createRow(0);
                        headerRow.createCell(0).setCellValue("Staff");
                        headerRow.createCell(1).setCellValue("Orders");
                        headerRow.createCell(2).setCellValue("Amount");
                        rowIdx = 1;
                        for (BusinessSummaryDto.StaffReportSummary sr : data.getStaffReports()) {
                                Row row = staffSheet.createRow(rowIdx++);
                                row.createCell(0).setCellValue(sr.getStaffName());
                                row.createCell(1).setCellValue(sr.getQuantity());
                                row.createCell(2).setCellValue(sr.getAmount().doubleValue());
                        }

                        // Sheet 9: Category
                        Sheet catSheet = workbook.createSheet("Category Report");
                        headerRow = catSheet.createRow(0);
                        headerRow.createCell(0).setCellValue("Category");
                        headerRow.createCell(1).setCellValue("Qty Sold");
                        headerRow.createCell(2).setCellValue("Sales Amount");
                        rowIdx = 1;
                        for (BusinessSummaryDto.CategoryReportSummary cr : data.getCategoryReports()) {
                                Row row = catSheet.createRow(rowIdx++);
                                row.createCell(0).setCellValue(cr.getCategoryName());
                                row.createCell(1).setCellValue(cr.getQuantity());
                                row.createCell(2).setCellValue(cr.getAmount().doubleValue());
                        }

                        // Sheet 10: Wastage
                        Sheet wastageSheet = workbook.createSheet("Wastage");
                        headerRow = wastageSheet.createRow(0);
                        headerRow.createCell(0).setCellValue("Total Wastage Cost");

                        rowIdx = 1;
                        Row wRow = wastageSheet.createRow(rowIdx++);
                        wRow.createCell(0).setCellValue(data.getWastage().getTotalWastageCost().doubleValue());

                        workbook.write(out);
                        return new ByteArrayInputStream(out.toByteArray());
                } catch (IOException e) {
                        throw new RuntimeException("Failed to export Excel data: " + e.getMessage());
                }
        }

        private void addRow(Sheet sheet, int rowIdx, String label, BigDecimal value) {
                Row row = sheet.createRow(rowIdx);
                row.createCell(0).setCellValue(label);
                row.createCell(1).setCellValue(value != null ? value.doubleValue() : 0.0);
        }

        public com.example.multi_tanent.pos.dto.report.DailySalesSummaryDto getDailySalesSummary(LocalDate date) {
                Tenant tenant = getCurrentTenant();
                OffsetDateTime start = date.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime();
                OffsetDateTime end = date.plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime()
                                .minusNanos(1);

                List<Sale> allSales = saleRepository.findByTenantIdAndInvoiceDateBetween(tenant.getId(), start, end);

                com.example.multi_tanent.pos.dto.report.DailySalesSummaryDto dto = new com.example.multi_tanent.pos.dto.report.DailySalesSummaryDto();
                dto.setDate(date.toString());
                dto.setTime(java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
                dto.setStoreName(tenant.getName() != null ? tenant.getName() : "My Store");

                // 1. Filter for valid sales (completed/paid/partial) vs cancelled
                List<Sale> validSales = allSales.stream()
                                .filter(s -> !"cancelled".equalsIgnoreCase(s.getStatus()))
                                .collect(Collectors.toList());
                List<Sale> cancelledSales = allSales.stream()
                                .filter(s -> "cancelled".equalsIgnoreCase(s.getStatus()))
                                .collect(Collectors.toList());

                // 2. Aggregates for Cash Report -> Sales Section
                long subTotalQty = validSales.size(); // Or sum items? Image implies "Sales" count. Let's use count of
                                                      // sales for
                                                      // now.
                long totalSubtotalCents = validSales.stream().mapToLong(Sale::getSubtotalCents).sum();
                long totalDiscountCents = validSales.stream()
                                .mapToLong(s -> s.getDiscountCents() != null ? s.getDiscountCents() : 0).sum();
                long totalDeliveryCents = validSales.stream()
                                .mapToLong(s -> s.getDeliveryCharge() != null ? s.getDeliveryCharge() : 0).sum();
                long totalTaxCents = validSales.stream().mapToLong(Sale::getTaxCents).sum();
                long totalBillCents = validSales.stream().mapToLong(Sale::getTotalCents).sum();

                com.example.multi_tanent.pos.dto.report.DailySalesSummaryDto.SalesSection salesSection = new com.example.multi_tanent.pos.dto.report.DailySalesSummaryDto.SalesSection();
                salesSection.setSubTotalQty(subTotalQty);
                salesSection.setSubTotalAmount(toBigDecimal(totalSubtotalCents));
                salesSection.setDiscount(toBigDecimal(totalDiscountCents));
                salesSection.setShippingCharge(toBigDecimal(totalDeliveryCents));
                salesSection.setVat(toBigDecimal(totalTaxCents));
                salesSection.setBillAmount(toBigDecimal(totalBillCents));

                // 3. Collection Section
                // Assuming "Net Collection" is total paid amount.
                // We need to check Payments if available, or assume TotalCents for
                // paid/completed sales.
                // For accurate collection, we should sum up Payment entities if they exist.
                // Since Sale entity has getPayments(), we use that if initialized.
                // However, standard sales fetch might not fetch payments eagerly.
                // Let's assume for this report we sum 'TotalCents' of valid sales as 'Net
                // Collection' for now,
                // or refine if Payment entity is available. The provided Sale entity has
                // List<Payment>.

                long totalCollectedCents = validSales.stream()
                                .mapToLong(s -> {
                                        if (s.getPayments() != null && !s.getPayments().isEmpty()) {
                                                return s.getPayments().stream()
                                                                .mapToLong(p -> p.getAmountCents() != null
                                                                                ? p.getAmountCents()
                                                                                : 0)
                                                                .sum();
                                        } else {
                                                // Fallback: if completed/paid, assume partial/full payment logic or
                                                // just 0 if
                                                // no payment record?
                                                // Usually if status is 'paid', total was collected.
                                                return "paid".equalsIgnoreCase(s.getPaymentStatus()) ? s.getTotalCents()
                                                                : 0;
                                        }
                                }).sum();

                com.example.multi_tanent.pos.dto.report.DailySalesSummaryDto.CollectionSection collectionSection = new com.example.multi_tanent.pos.dto.report.DailySalesSummaryDto.CollectionSection();
                collectionSection.setNetCollection(toBigDecimal(totalCollectedCents));
                collectionSection.setTipAmount(BigDecimal.ZERO); // Not tracked
                collectionSection.setOnAccount(BigDecimal.ZERO); // Not tracked specifically yet

                // 4. Others Section
                com.example.multi_tanent.pos.dto.report.DailySalesSummaryDto.OthersSection othersSection = new com.example.multi_tanent.pos.dto.report.DailySalesSummaryDto.OthersSection();
                othersSection.setVatQty(0L); // Usually count of tax items?
                othersSection.setVatAmount(toBigDecimal(totalTaxCents));
                othersSection.setDeliveryChargesQty(0L);
                othersSection.setDeliveryChargesAmount(toBigDecimal(totalDeliveryCents));
                othersSection.setTotal(toBigDecimal(totalTaxCents + totalDeliveryCents));

                // 5. Discount Details
                com.example.multi_tanent.pos.dto.report.DailySalesSummaryDto.DiscountDetailsSection discountSection = new com.example.multi_tanent.pos.dto.report.DailySalesSummaryDto.DiscountDetailsSection();
                discountSection.setTotal(toBigDecimal(totalDiscountCents));

                com.example.multi_tanent.pos.dto.report.DailySalesSummaryDto.CashReportSection cashReport = new com.example.multi_tanent.pos.dto.report.DailySalesSummaryDto.CashReportSection();
                cashReport.setSales(salesSection);
                cashReport.setCollection(collectionSection);
                cashReport.setOthers(othersSection);
                cashReport.setDiscountDetails(discountSection);
                dto.setCashReport(cashReport);

                // 6. Tax Report
                // Group sale items by tax rate
                // We need to iterate all items of valid sales.
                Map<String, List<com.example.multi_tanent.pos.entity.SaleItem>> itemsByTax = validSales.stream()
                                .flatMap(s -> s.getItems().stream())
                                .collect(Collectors.groupingBy(item -> {
                                        if (item.getProductVariant() != null
                                                        && item.getProductVariant().getTaxRate() != null) {
                                                return item.getProductVariant().getTaxRate().getName() + " ("
                                                                + item.getProductVariant().getTaxRate().getPercent()
                                                                + "%)";
                                        }
                                        return "No Tax";
                                }));

                List<com.example.multi_tanent.pos.dto.report.DailySalesSummaryDto.TaxReportItem> taxReports = new ArrayList<>();
                itemsByTax.forEach((rateName, items) -> {
                        long taxableAmountCents = items.stream()
                                        .mapToLong(com.example.multi_tanent.pos.entity.SaleItem::getLineTotalCents)
                                        .sum();
                        long taxValCents = items.stream()
                                        .mapToLong(com.example.multi_tanent.pos.entity.SaleItem::getTaxCents)
                                        .sum();

                        taxReports.add(new com.example.multi_tanent.pos.dto.report.DailySalesSummaryDto.TaxReportItem(
                                        rateName,
                                        toBigDecimal(taxableAmountCents),
                                        toBigDecimal(taxValCents)));
                });
                dto.setTaxReports(taxReports);

                // 7. POS Report
                com.example.multi_tanent.pos.dto.report.DailySalesSummaryDto.PosReportSection posReport = new com.example.multi_tanent.pos.dto.report.DailySalesSummaryDto.PosReportSection();
                posReport.setGrossSales(salesSection.getBillAmount()); // Or Subtotal + Tax? Bill Amount is Total.
                posReport.setDeduction(BigDecimal.ZERO); // Returns/Wastage could go here
                posReport.setNetSale(salesSection.getBillAmount());
                posReport.setTotal(salesSection.getBillAmount());
                dto.setPosReport(posReport);

                // 8. Cancellation Report
                // Currently we only have "cancelled" status sales.
                List<com.example.multi_tanent.pos.dto.report.DailySalesSummaryDto.CancellationReportItem> cancelledItems = cancelledSales
                                .stream()
                                .map(s -> new com.example.multi_tanent.pos.dto.report.DailySalesSummaryDto.CancellationReportItem(
                                                s.getInvoiceNo(),
                                                "Cancelled Sale", // or Customer Name
                                                (long) s.getItems().size(),
                                                toBigDecimal(s.getTotalCents())))
                                .collect(Collectors.toList());
                dto.setCancellationReports(cancelledItems);

                return dto;
        }

        public List<com.example.multi_tanent.pos.dto.report.SalesByHourDto> getSalesByHour(LocalDate fromDate,
                        LocalDate toDate) {
                Tenant tenant = getCurrentTenant();
                OffsetDateTime start = fromDate.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime();
                OffsetDateTime end = toDate.plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime()
                                .minusNanos(1);

                List<Sale> allSales = saleRepository.findByTenantIdAndInvoiceDateBetween(tenant.getId(), start, end);

                // Filter valid sales
                List<Sale> validSales = allSales.stream()
                                .filter(s -> !"cancelled".equalsIgnoreCase(s.getStatus()))
                                .collect(Collectors.toList());

                // Initialize map for 24 hours
                Map<Integer, com.example.multi_tanent.pos.dto.report.SalesByHourDto> hourlyData = new java.util.HashMap<>();
                for (int i = 0; i < 24; i++) {
                        String hourStr = String.format("%02d:00", i);
                        hourlyData.put(i,
                                        new com.example.multi_tanent.pos.dto.report.SalesByHourDto(hourStr, 0L, 0L,
                                                        BigDecimal.ZERO));
                }

                // Aggregate
                for (Sale sale : validSales) {
                        // Convert to system default zone to get local hour
                        int hour = sale.getInvoiceDate().atZoneSameInstant(ZoneId.systemDefault()).getHour();
                        com.example.multi_tanent.pos.dto.report.SalesByHourDto dto = hourlyData.get(hour);

                        if (dto != null) {
                                dto.setSalesCount(dto.getSalesCount() + 1);
                                dto.setAmount(dto.getAmount().add(toBigDecimal(sale.getTotalCents())));

                                long qty = sale.getItems().stream()
                                                .mapToLong(com.example.multi_tanent.pos.entity.SaleItem::getQuantity)
                                                .sum();
                                dto.setQuantity(dto.getQuantity() + qty);
                        }
                }

                return new ArrayList<>(hourlyData.values()).stream()
                                .sorted(java.util.Comparator.comparing(
                                                com.example.multi_tanent.pos.dto.report.SalesByHourDto::getHour))
                                .collect(Collectors.toList());
        }

        @Autowired
        private com.example.multi_tanent.pos.repository.CashRegisterRepository cashRegisterRepository;

        public List<com.example.multi_tanent.pos.dto.report.ClosingReportDto> getClosingReports(LocalDate fromDate,
                        LocalDate toDate) {
                Tenant tenant = getCurrentTenant();
                OffsetDateTime start = fromDate.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime();
                OffsetDateTime end = toDate.plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime()
                                .minusNanos(1);

                List<com.example.multi_tanent.pos.entity.CashRegister> registers = cashRegisterRepository
                                .findByTenantIdAndOpeningTimeBetween(tenant.getId(), start, end);

                return registers.stream().map(cr -> {
                        com.example.multi_tanent.pos.dto.report.ClosingReportDto dto = new com.example.multi_tanent.pos.dto.report.ClosingReportDto();
                        dto.setId(cr.getId());
                        dto.setOpeningDate(cr.getOpeningTime() != null ? cr.getOpeningTime().toString() : "");
                        dto.setOpeningFloat(cr.getOpeningFloat());
                        // Deriving "Running Date" from opening time
                        dto.setRunningDate(cr.getOpeningTime() != null ? cr.getOpeningTime().toLocalDate().toString()
                                        : "");
                        dto.setClosingDate(cr.getClosingTime() != null ? cr.getClosingTime().toString() : "");
                        dto.setExpectedCashAmount(cr.getExpectedCashAmount());
                        dto.setCountedCashAmount(cr.getCountedCashAmount());
                        dto.setClosedCashDifference(cr.getClosedCashDifference());
                        dto.setNotes(cr.getNotes());
                        return dto;
                }).collect(Collectors.toList());
        }

        public java.util.List<com.example.multi_tanent.pos.dto.report.SalesSupportReportItemDto> getSalesSupportReport(
                        LocalDate fromDate, LocalDate toDate, Long storeId) {
                Tenant tenant = getCurrentTenant();
                OffsetDateTime start = fromDate.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime();
                OffsetDateTime end = toDate.plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime()
                                .minusNanos(1);

                // Fetch sales
                List<Sale> sales;
                if (storeId != null) {
                        sales = saleRepository.findByTenantIdAndStoreIdAndInvoiceDateBetween(tenant.getId(), storeId,
                                        start, end);
                } else {
                        sales = saleRepository.findByTenantIdAndInvoiceDateBetween(tenant.getId(), start, end);
                }

                // Map to DTO
                return sales.stream().map(sale -> {
                        com.example.multi_tanent.pos.dto.report.SalesSupportReportItemDto dto = new com.example.multi_tanent.pos.dto.report.SalesSupportReportItemDto();
                        dto.setRef(sale.getInvoiceNo());
                        dto.setOrderType(sale.getOrderType() != null ? sale.getOrderType().name() : "");
                        dto.setCustomer(sale.getCustomer() != null ? sale.getCustomer().getName() : "Walk-in");
                        dto.setOrderTakenBy(sale.getUser() != null ? sale.getUser().getName() : "Unknown");

                        // Date & Time
                        if (sale.getInvoiceDate() != null) {
                                // Convert to local date/time for display
                                java.time.LocalDateTime localDateTime = sale.getInvoiceDate()
                                                .atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
                                dto.setOrderDate(localDateTime.toLocalDate());
                                dto.setOrderTime(localDateTime.toLocalTime());
                        }

                        dto.setSalesSource(sale.getSalesSource() != null ? sale.getSalesSource() : "");
                        dto.setSalesSourceReference(sale.getOrderId()); // Using Order ID as reference
                        dto.setSubTotal(toBigDecimal(sale.getSubtotalCents()));
                        dto.setDiscount(toBigDecimal(sale.getDiscountCents()));
                        dto.setVat(toBigDecimal(sale.getTaxCents()));
                        dto.setShippingCharge(toBigDecimal(sale.getDeliveryCharge()));
                        dto.setOrderTotal(toBigDecimal(sale.getTotalCents()));

                        // Customer Paid Amount
                        BigDecimal paidAmount = BigDecimal.ZERO;
                        if (sale.getPayments() != null && !sale.getPayments().isEmpty()) {
                                paidAmount = sale.getPayments().stream()
                                                .map(p -> toBigDecimal(p.getAmountCents()))
                                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        } else if ("paid".equalsIgnoreCase(sale.getPaymentStatus())) {
                                paidAmount = toBigDecimal(sale.getTotalCents());
                        }
                        dto.setCustomerPaidAmount(paidAmount);

                        // Returned Amount - Currently assuming 0 unless status is refunded or we verify
                        // stock movements for return
                        dto.setReturnedAmount(BigDecimal.ZERO); // Placeholder

                        // Payment Method
                        String methods = "";
                        if (sale.getPayments() != null && !sale.getPayments().isEmpty()) {
                                methods = sale.getPayments().stream()
                                                .map(p -> p.getMethod())
                                                .distinct()
                                                .collect(Collectors.joining(", "));
                        }
                        dto.setPaymentMethod(methods);

                        dto.setBillReceivedBy(sale.getUser() != null ? sale.getUser().getName() : "Unknown"); // Default
                                                                                                              // to
                                                                                                              // order
                                                                                                              // taker

                        return dto;
                }).collect(Collectors.toList());
        }

        public java.util.List<com.example.multi_tanent.pos.dto.report.SalesForecastingReportItemDto> getSalesForecastingReport(
                        LocalDate fromDate, LocalDate toDate, Long storeId) {
                Tenant tenant = getCurrentTenant();
                List<com.example.multi_tanent.pos.dto.report.SalesForecastingReportItemDto> report = new ArrayList<>();

                // Normalize dates to start of month
                LocalDate startMonth = fromDate.withDayOfMonth(1);
                LocalDate endMonth = toDate.withDayOfMonth(1);

                LocalDate current = startMonth;
                while (!current.isAfter(endMonth)) {
                        // Calculate month range
                        OffsetDateTime monthStart = current.atStartOfDay().atZone(ZoneId.systemDefault())
                                        .toOffsetDateTime();
                        OffsetDateTime monthEnd = current.plusMonths(1).atStartOfDay().atZone(ZoneId.systemDefault())
                                        .toOffsetDateTime().minusNanos(1);

                        // 1. Get Actual Sales for this month
                        List<Sale> sales;
                        if (storeId != null) {
                                sales = saleRepository.findByTenantIdAndStoreIdAndInvoiceDateBetween(tenant.getId(),
                                                storeId, monthStart, monthEnd);
                        } else {
                                sales = saleRepository.findByTenantIdAndInvoiceDateBetween(tenant.getId(), monthStart,
                                                monthEnd);
                        }

                        BigDecimal actualTotal = sales.stream()
                                        .filter(s -> "completed".equalsIgnoreCase(s.getStatus())
                                                        || "paid".equalsIgnoreCase(s.getPaymentStatus()))
                                        .map(s -> toBigDecimal(s.getTotalCents()))
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                        // 2. Get Forecast
                        BigDecimal forecastTotal = BigDecimal.ZERO;
                        if (storeId != null) {
                                // specific store forecast
                                com.example.multi_tanent.pos.entity.SalesForecast forecast = salesForecastRepository
                                                .findByTenantIdAndStoreIdAndMonth(tenant.getId(), storeId, current)
                                                .orElse(null);
                                if (forecast != null) {
                                        forecastTotal = forecast.getForecastAmount();
                                }
                        } else {
                                // Aggregate all stores forecasts for tenant?
                                // Or just 0 if no store selected (depends on UI). The UI shows "Company Branch"
                                // dropdown.
                                // Assuming if storeId is null, we sum all forecasts for that month?
                                List<com.example.multi_tanent.pos.entity.SalesForecast> forecasts = salesForecastRepository
                                                .findByTenantIdAndStoreIdAndMonthBetween(tenant.getId(), null, current,
                                                                current);
                                // The repository method I defined earlier has storeId param. I should add a
                                // method for just tenant/month.
                                // Or iterate manually.
                                // Let's assume for now user selects a store. If not, we iterate known stores or
                                // skip.
                                // Actually, filtering `findByTenantIdAndStoreIdAndMonthBetween` with
                                // storeId=null won't work in JPA usually if param is null.
                                // I'll stick to 0 if storeId is missing, strictly following UI which seems to
                                // require branch selection.
                        }

                        // 3. Calculate Percentage
                        BigDecimal achievedPct = BigDecimal.ZERO;
                        if (forecastTotal.compareTo(BigDecimal.ZERO) > 0) {
                                achievedPct = actualTotal.divide(forecastTotal, 2, java.math.RoundingMode.HALF_UP)
                                                .multiply(BigDecimal.valueOf(100));
                        }

                        com.example.multi_tanent.pos.dto.report.SalesForecastingReportItemDto item = new com.example.multi_tanent.pos.dto.report.SalesForecastingReportItemDto();
                        item.setMonthYear(current.toString().substring(0, 7)); // YYYY-MM
                        item.setActualSales(actualTotal);
                        item.setForecastedSales(forecastTotal);
                        item.setAchievedPercentage(achievedPct);

                        report.add(item);

                        current = current.plusMonths(1);
                }

                return report;
        }

        public java.util.List<com.example.multi_tanent.pos.dto.report.DeliveryDriverReportItemDto> getDeliveryDriverReport(
                        LocalDate fromDate, LocalDate toDate, Long driverId) {
                Tenant tenant = getCurrentTenant();
                OffsetDateTime start = fromDate.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime();
                OffsetDateTime end = toDate.plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime()
                                .minusNanos(1);

                List<Sale> sales;
                // Basic fetching - refinement needed if we want to filter by driver
                // specifically in DB
                // But SaleRepository might not have that method yet.
                // We can fetch all sales in range and filter in memory since volume per date
                // range is manageable,
                // or we should update repository.
                // For now, let's fetch by date and filter stream.
                sales = saleRepository.findByTenantIdAndInvoiceDateBetween(tenant.getId(), start, end);

                return sales.stream()
                                .filter(s -> s.getDriver() != null) // Only sales with drivers
                                .filter(s -> driverId == null || s.getDriver().getId().equals(driverId))
                                .map(sale -> {
                                        com.example.multi_tanent.pos.dto.report.DeliveryDriverReportItemDto dto = new com.example.multi_tanent.pos.dto.report.DeliveryDriverReportItemDto();
                                        dto.setOrderNo(sale.getInvoiceNo()); // Or orderId
                                        dto.setStatus(sale.getStatus());

                                        if (sale.getInvoiceDate() != null) {
                                                dto.setOrderDate(sale.getInvoiceDate()
                                                                .atZoneSameInstant(ZoneId.systemDefault())
                                                                .toLocalDate());
                                        }

                                        if (sale.getDispatchedTime() != null) {
                                                dto.setDispatchedTime(sale.getDispatchedTime()
                                                                .atZoneSameInstant(ZoneId.systemDefault())
                                                                .toLocalTime());
                                        }

                                        if (sale.getDeliveredTime() != null) {
                                                dto.setDeliveredTime(sale.getDeliveredTime()
                                                                .atZoneSameInstant(ZoneId.systemDefault())
                                                                .toLocalTime());
                                        }

                                        // ridingTime = delivered - dispatched
                                        if (sale.getDispatchedTime() != null && sale.getDeliveredTime() != null) {
                                                java.time.Duration duration = java.time.Duration.between(
                                                                sale.getDispatchedTime(), sale.getDeliveredTime());
                                                long minutes = duration.toMinutes();
                                                long hours = minutes / 60;
                                                long remainingMinutes = minutes % 60;
                                                String ridingTime = String.format("%02d:%02d", hours, remainingMinutes);
                                                dto.setRidingTime(ridingTime);
                                        } else {
                                                dto.setRidingTime("-");
                                        }

                                        dto.setTotalAmount(toBigDecimal(sale.getTotalCents()));

                                        // paymentType
                                        String methods = "";
                                        if (sale.getPayments() != null && !sale.getPayments().isEmpty()) {
                                                methods = sale.getPayments().stream()
                                                                .map(p -> p.getMethod())
                                                                .distinct()
                                                                .collect(Collectors.joining(", "));
                                        } else {
                                                methods = sale.getPaymentStatus();
                                        }
                                        dto.setPaymentType(methods);

                                        dto.setAddress(sale.getDeliveryAddress() != null ? sale.getDeliveryAddress()
                                                        : (sale.getCustomer() != null
                                                                        && sale.getCustomer().getPhone() != null
                                                                                        ? sale.getCustomer().getPhone()
                                                                                        : "")); // Fallback or empty

                                        if (sale.getExpectedDeliveryTime() != null) {
                                                dto.setExpectedDeliveryTime(sale.getExpectedDeliveryTime()
                                                                .atZoneSameInstant(ZoneId.systemDefault())
                                                                .toLocalTime());
                                        }

                                        return dto;
                                })
                                .collect(Collectors.toList());
        }

        public java.util.List<com.example.multi_tanent.pos.dto.report.SalesPaymentReportItemDto> getSalesPaymentReport(
                        LocalDate fromDate, LocalDate toDate, Long storeId, String paymentMethod) {
                Tenant tenant = getCurrentTenant();
                OffsetDateTime start = fromDate.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime();
                OffsetDateTime end = toDate.plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime()
                                .minusNanos(1);

                List<com.example.multi_tanent.pos.entity.Payment> payments;
                if (storeId != null) {
                        payments = paymentRepository.findBySaleTenantIdAndSaleStoreIdAndCreatedAtBetween(tenant.getId(),
                                        storeId, start, end);
                } else {
                        payments = paymentRepository.findBySaleTenantIdAndCreatedAtBetween(tenant.getId(), start, end);
                }

                // Filter by Payment Method if provided
                if (paymentMethod != null && !paymentMethod.isEmpty() && !"All".equalsIgnoreCase(paymentMethod)) {
                        payments = payments.stream()
                                        .filter(p -> p.getMethod().equalsIgnoreCase(paymentMethod))
                                        .collect(Collectors.toList());
                }

                return payments.stream().map(payment -> {
                        com.example.multi_tanent.pos.dto.report.SalesPaymentReportItemDto dto = new com.example.multi_tanent.pos.dto.report.SalesPaymentReportItemDto();
                        dto.setType("Sale"); // Default
                        if (payment.getSale() != null) {
                                dto.setOrderRef(payment.getSale().getInvoiceNo());
                                if (payment.getSale().getInvoiceDate() != null) {
                                        dto.setOrderDate(payment.getSale().getInvoiceDate()
                                                        .atZoneSameInstant(ZoneId.systemDefault())
                                                        .toLocalDate());
                                }
                                if (payment.getSale().getCustomer() != null) {
                                        dto.setCustomer(payment.getSale().getCustomer().getName());
                                } else {
                                        dto.setCustomer("Walk-in");
                                }
                        }
                        dto.setReference(payment.getReference());
                        dto.setPaymentDate(
                                        payment.getCreatedAt().atZoneSameInstant(ZoneId.systemDefault()).toLocalDate());
                        dto.setCustomerPaidAmount(toBigDecimal(payment.getAmountCents()));
                        dto.setAmount(toBigDecimal(payment.getAmountCents()));
                        dto.setCustomerReturnedAmount(BigDecimal.ZERO); // Placeholder
                        dto.setCurrency("USD"); // Default or fetch from config
                        dto.setBankName(""); // Not stored
                        dto.setNewPayMethod(payment.getMethod());
                        dto.setCardNumber(""); // Not stored

                        return dto;
                }).collect(Collectors.toList());
        }

        public void transferUserActivity(com.example.multi_tanent.pos.dto.report.UserTransferRequestDto request) {
                Tenant tenant = getCurrentTenant();
                // Validate Stores
                com.example.multi_tanent.spersusers.enitity.Store toStore = storeRepository
                                .findById(request.getToStoreId())
                                .orElseThrow(() -> new IllegalArgumentException("Target Store not found"));

                // Fetch sales for the user in the source store
                List<Sale> sales = saleRepository.findByTenantIdAndStoreIdAndUserId(tenant.getId(),
                                request.getFromStoreId(), request.getUserId());

                if (sales.isEmpty()) {
                        throw new IllegalArgumentException("No sales found for this user in the source store.");
                }

                // Update store for each sale
                for (Sale sale : sales) {
                        sale.setStore(toStore);
                }

                saleRepository.saveAll(sales);
        }

        public List<com.example.multi_tanent.pos.dto.report.SalesSourceReportItemDto> getSalesSourceReport(
                        LocalDate fromDate, LocalDate toDate, Long storeId, String source, String reference) {
                Tenant tenant = getCurrentTenant();
                OffsetDateTime start = fromDate.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime();
                OffsetDateTime end = toDate.plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime()
                                .minusNanos(1);

                String sourceFilter = (source != null && !source.isBlank() && !"All".equalsIgnoreCase(source)) ? source
                                : null;
                String refFilter = (reference != null && !reference.isBlank()) ? reference : null;

                List<Sale> sales = saleRepository.findSalesBySourceFilters(tenant.getId(), storeId, start, end,
                                sourceFilter, refFilter);

                return sales.stream().map(sale -> {
                        com.example.multi_tanent.pos.dto.report.SalesSourceReportItemDto dto = new com.example.multi_tanent.pos.dto.report.SalesSourceReportItemDto();
                        dto.setOrderNo(sale.getInvoiceNo());
                        dto.setReference(sale.getOrderId()); // Assuming Ref column maps to OrderId or custom Ref
                        dto.setSource(sale.getSalesSource());
                        dto.setSalesSourceReference(sale.getSalesSourceReference());
                        dto.setCustomerName(sale.getCustomer() != null ? sale.getCustomer().getName() : "Walk-in");
                        dto.setDate(sale.getInvoiceDate().toLocalDateTime());
                        dto.setSalesTotalAmount(toBigDecimal(sale.getTotalCents()));
                        return dto;
                }).collect(Collectors.toList());
        }

        public List<com.example.multi_tanent.pos.dto.report.TotalSalesReportItemDto> getTotalSalesReport(
                        LocalDate fromDate, LocalDate toDate, Long storeId) {
                Tenant tenant = getCurrentTenant();
                OffsetDateTime start = fromDate.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime();
                OffsetDateTime end = toDate.plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime()
                                .minusNanos(1);

                List<Sale> sales;
                if (storeId != null) {
                        sales = saleRepository.findByTenantIdAndStoreIdAndInvoiceDateBetween(tenant.getId(), storeId,
                                        start, end);
                } else {
                        sales = saleRepository.findByTenantIdAndInvoiceDateBetween(tenant.getId(), start, end);
                }

                return sales.stream().map(sale -> {
                        com.example.multi_tanent.pos.dto.report.TotalSalesReportItemDto dto = new com.example.multi_tanent.pos.dto.report.TotalSalesReportItemDto();
                        dto.setRef(sale.getInvoiceNo());
                        dto.setOrderType(sale.getOrderType() != null ? sale.getOrderType().name() : "");
                        dto.setLocation(sale.getStore() != null ? sale.getStore().getName() : "");
                        dto.setCustomer(sale.getCustomer() != null ? sale.getCustomer().getName() : "Walk-in");
                        dto.setDebtor("unpaid".equalsIgnoreCase(sale.getPaymentStatus()) ? dto.getCustomer() : "");
                        dto.setSalesSource(sale.getSalesSource());
                        dto.setSalesSourceReference(sale.getSalesSourceReference());

                        if (sale.getInvoiceDate() != null) {
                                dto.setOrderDate(sale.getInvoiceDate().atZoneSameInstant(ZoneId.systemDefault())
                                                .toLocalDate());
                                dto.setOrderTime(sale.getInvoiceDate().atZoneSameInstant(ZoneId.systemDefault())
                                                .toLocalTime());
                        }

                        dto.setSubTotal(toBigDecimal(sale.getSubtotalCents()));
                        dto.setDiscount(toBigDecimal(sale.getDiscountCents()));
                        dto.setVat(toBigDecimal(sale.getTaxCents()));
                        dto.setShippingCharge(toBigDecimal(sale.getDeliveryCharge()));
                        dto.setOrderTotal(toBigDecimal(sale.getTotalCents()));

                        BigDecimal paid = sale.getPayments().stream()
                                        .map(p -> toBigDecimal(p.getAmountCents()))
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                        dto.setCustomerPaidAmount(paid);

                        dto.setReturnedAmount(paid.compareTo(dto.getOrderTotal()) > 0
                                        ? paid.subtract(dto.getOrderTotal())
                                        : BigDecimal.ZERO);

                        String methods = sale.getPayments().stream()
                                        .map(com.example.multi_tanent.pos.entity.Payment::getMethod)
                                        .distinct()
                                        .collect(Collectors.joining(", "));
                        dto.setPaymentMethod(methods);

                        // Settlement time from last payment
                        sale.getPayments().stream()
                                        .max(java.util.Comparator
                                                        .comparing(com.example.multi_tanent.pos.entity.Payment::getCreatedAt))
                                        .ifPresent(p -> dto.setSettlementTime(
                                                        p.getCreatedAt().atZoneSameInstant(ZoneId.systemDefault())
                                                                        .toLocalTime()));

                        return dto;
                }).collect(Collectors.toList());
        }

        public Object getSalesStatusReport(LocalDate fromDate, LocalDate toDate, String status) {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'getSalesStatusReport'");
        }

        public java.util.List<com.example.multi_tanent.pos.dto.report.PaymentChangeReportItemDto> getPaymentChangeReport(
                        LocalDate fromDate, LocalDate toDate, Long storeId, Long userId, String orderNo) {
                Tenant tenant = getCurrentTenant();
                OffsetDateTime start = fromDate.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime();
                OffsetDateTime end = toDate.plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime()
                                .minusNanos(1);

                java.util.List<com.example.multi_tanent.pos.entity.PaymentChangeLog> logs;

                if (storeId != null) {
                        logs = paymentChangeLogRepository.findByTenantIdAndStoreIdAndCreatedAtBetween(tenant.getId(),
                                        storeId, start, end);
                } else if (userId != null) {
                        logs = paymentChangeLogRepository.findByTenantIdAndUserIdAndCreatedAtBetween(tenant.getId(),
                                        userId, start, end);
                } else {
                        logs = paymentChangeLogRepository.findByTenantIdAndCreatedAtBetween(tenant.getId(), start, end);
                }

                if (userId != null) {
                        logs = logs.stream().filter(l -> l.getUser() != null && l.getUser().getId().equals(userId))
                                        .collect(java.util.stream.Collectors.toList());
                }

                return logs.stream().map(log -> {
                        Sale sale = saleRepository.findById(log.getSaleId()).orElse(null);
                        if (orderNo != null && !orderNo.isBlank()) {
                                if (sale == null || !sale.getInvoiceNo().contains(orderNo)) {
                                        return null;
                                }
                        }

                        com.example.multi_tanent.pos.dto.report.PaymentChangeReportItemDto dto = new com.example.multi_tanent.pos.dto.report.PaymentChangeReportItemDto();
                        dto.setOrderNo(sale != null ? sale.getInvoiceNo() : "Unknown");
                        dto.setTransNo(String.valueOf(log.getPaymentId()));
                        dto.setUser(log.getUser() != null ? log.getUser().getName() : "Unknown");
                        dto.setCardAssignedUser(log.getCardAssignedUser());
                        dto.setBranch(log.getStore() != null ? log.getStore().getName() : "");
                        dto.setFromAccount(log.getFromMethod());
                        dto.setToAccount(log.getToMethod());
                        dto.setDate(log.getCreatedAt().toLocalDate());
                        return dto;
                }).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toList());
        }

        public java.util.List<com.example.multi_tanent.pos.dto.report.SalesDiscountReportItemDto> getSalesDiscountReport(
                        LocalDate fromDate, LocalDate toDate, Long storeId, String discountType) {
                Tenant tenant = getCurrentTenant();
                OffsetDateTime start = fromDate.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime();
                OffsetDateTime end = toDate.plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime()
                                .minusNanos(1);

                java.util.List<Sale> sales;
                if (storeId != null) {
                        sales = saleRepository.findByTenantIdAndStoreIdAndInvoiceDateBetween(tenant.getId(), storeId,
                                        start, end);
                } else {
                        sales = saleRepository.findByTenantIdAndInvoiceDateBetween(tenant.getId(), start, end);
                }

                // Filter for sales with discount
                java.util.stream.Stream<Sale> stream = sales.stream()
                                .filter(s -> s.getDiscountCents() != null && s.getDiscountCents() > 0);

                if (discountType != null && !discountType.isBlank() && !"All".equalsIgnoreCase(discountType)) {
                        stream = stream.filter(s -> discountType.equalsIgnoreCase(s.getDiscountType()));
                }

                return stream.map(sale -> {
                        com.example.multi_tanent.pos.dto.report.SalesDiscountReportItemDto dto = new com.example.multi_tanent.pos.dto.report.SalesDiscountReportItemDto();
                        dto.setBranch(sale.getStore() != null ? sale.getStore().getName() : "");
                        dto.setDiscountType(sale.getDiscountType() != null ? sale.getDiscountType() : "General");
                        dto.setCount(1L); // Detail row count
                        // Order Amount After Discount is TotalCents
                        dto.setOrderAmountAfterDiscount(toBigDecimal(sale.getTotalCents()));
                        dto.setDiscountAmount(toBigDecimal(sale.getDiscountCents()));

                        if (sale.getInvoiceDate() != null) {
                                dto.setOrderDate(sale.getInvoiceDate().toLocalDate());
                                dto.setOrderHour(String.format("%02d", sale.getInvoiceDate().getHour()));
                        }
                        return dto;
                }).collect(java.util.stream.Collectors.toList());
        }

        public java.util.List<com.example.multi_tanent.pos.dto.report.CancellationReportItemDto> getCancellationReport(
                        LocalDate fromDate, LocalDate toDate, Long storeId) {
                Tenant tenant = getCurrentTenant();
                OffsetDateTime start = fromDate.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime();
                OffsetDateTime end = toDate.plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime()
                                .minusNanos(1);

                java.util.List<Sale> sales;
                if (storeId != null) {
                        sales = saleRepository.findByTenantIdAndStoreIdAndInvoiceDateBetween(tenant.getId(), storeId,
                                        start, end);
                } else {
                        sales = saleRepository.findByTenantIdAndInvoiceDateBetween(tenant.getId(), start, end);
                }

                return sales.stream()
                                .filter(s -> "cancelled".equalsIgnoreCase(s.getStatus()))
                                .map(sale -> {
                                        com.example.multi_tanent.pos.dto.report.CancellationReportItemDto dto = new com.example.multi_tanent.pos.dto.report.CancellationReportItemDto();
                                        dto.setBillNo(sale.getInvoiceNo());
                                        dto.setRef(sale.getOrderId());
                                        dto.setOrderType(sale.getOrderType() != null ? sale.getOrderType().name() : "");
                                        dto.setPos(sale.getStore() != null ? sale.getStore().getName() : "");
                                        dto.setOrderTakenBy(sale.getUser() != null ? sale.getUser().getName() : "");
                                        dto.setCardUser(sale.getUser() != null ? sale.getUser().getName() : "");
                                        dto.setDate(sale.getInvoiceDate() != null ? sale.getInvoiceDate().toLocalDate()
                                                        : null);
                                        dto.setReason(sale.getCancellationReason());
                                        dto.setAmount(toBigDecimal(sale.getTotalCents()));

                                        if (sale.getCreatedAt() != null) {
                                                dto.setOrderPlacedTime(sale.getCreatedAt()
                                                                .atZoneSameInstant(ZoneId.systemDefault())
                                                                .toLocalTime());
                                        }
                                        if (sale.getCancelledTime() != null) {
                                                dto.setOrderCancelledTime(sale.getCancelledTime()
                                                                .atZoneSameInstant(ZoneId.systemDefault())
                                                                .toLocalTime());
                                        }

                                        String methods = sale.getPayments().stream()
                                                        .map(com.example.multi_tanent.pos.entity.Payment::getMethod)
                                                        .distinct()
                                                        .collect(java.util.stream.Collectors.joining(", "));
                                        dto.setPaymentMethod(methods);

                                        return dto;
                                }).collect(java.util.stream.Collectors.toList());
        }

        public List<com.example.multi_tanent.pos.dto.report.VoidReportItemDto> getVoidReport(
                        LocalDate fromDate, LocalDate toDate, Long storeId) {
                Tenant tenant = getCurrentTenant();
                OffsetDateTime start = fromDate.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime();
                OffsetDateTime end = toDate.plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime()
                                .minusNanos(1);

                // Fetch sales
                List<Sale> sales;
                if (storeId != null) {
                        sales = saleRepository.findByTenantIdAndStoreIdAndInvoiceDateBetween(tenant.getId(), storeId,
                                        start, end);
                } else {
                        sales = saleRepository.findByTenantIdAndInvoiceDateBetween(tenant.getId(), start, end);
                }

                // Filter by "void" status and map to DTO
                return sales.stream()
                                .filter(s -> "void".equalsIgnoreCase(s.getStatus()))
                                .map(s -> {
                                        long qty = s.getItems().stream()
                                                        .mapToLong(com.example.multi_tanent.pos.entity.SaleItem::getQuantity)
                                                        .sum();

                                        return new com.example.multi_tanent.pos.dto.report.VoidReportItemDto(
                                                        s.getInvoiceNo(),
                                                        s.getOrderType() != null ? s.getOrderType().name() : "",
                                                        s.getSalesSource() != null ? s.getSalesSource() : "POS",
                                                        s.getUser() != null ? s.getUser().getName() : "Unknown",
                                                        s.getCustomer() != null ? s.getCustomer().getName() : "Walk-in",
                                                        qty,
                                                        toBigDecimal(s.getTotalCents()));
                                })
                                .collect(Collectors.toList());
        }

        public List<com.example.multi_tanent.pos.dto.report.ItemMovementReportItemDto> getItemMovementReport(
                        LocalDate fromDate, LocalDate toDate, Long storeId, String itemName) {
                Tenant tenant = getCurrentTenant();
                OffsetDateTime start = fromDate.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime();
                OffsetDateTime end = toDate.plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime()
                                .minusNanos(1);

                List<com.example.multi_tanent.pos.entity.StockMovement> movements = stockMovementRepository
                                .findItemMovements(tenant.getId(), start, end, storeId, itemName);

                return movements.stream()
                                .map(sm -> {
                                        String stockId = sm.getId().toString();
                                        String name = (sm.getProductVariant() != null
                                                        && sm.getProductVariant().getProduct() != null)
                                                                        ? sm.getProductVariant().getProduct().getName()
                                                                        : "Unknown";
                                        // Append SKU as variant identifier
                                        if (sm.getProductVariant() != null && sm.getProductVariant().getSku() != null) {
                                                name += " (" + sm.getProductVariant().getSku() + ")";
                                        }

                                        String description = (sm.getProductVariant() != null
                                                        && sm.getProductVariant().getProduct() != null)
                                                                        ? sm.getProductVariant().getProduct()
                                                                                        .getDescription()
                                                                        : "";

                                        // Qty Out usually implies outgoing. If changeQuantity is negative, it's out.
                                        // If the report lists ALL movements, we might just show abs value and maybe
                                        // indicate direction?
                                        // The UI column says "Qty Out".
                                        // If user wants only OUT movements, we should filter.
                                        // But usually "Movement Report" shows flow.
                                        // Let's assume we show the signed value or absolute value.
                                        // Given "Qty Out" label, maybe it only wants outgoing?
                                        // "Item movement" generic title usually implies history.
                                        // Let's map quantity as is, or abs if the column is specifically 'Qty Out'.
                                        // If the report is generic Item Movement, maybe distinct columns 'In' and 'Out'
                                        // are better,
                                        // but the mockup has one 'Qty Out' column?
                                        // Wait, looking at the image 1 (Void report) and 2 (void report again)...
                                        // Wait, user said "add item movement report... by seeing images".
                                        // I need to check the NEW uploaded images.
                                        // Image 0_1766140427407.png -> "Item movement", "Qty Out", "Cost".
                                        // So it seems to target outgoing items? Or maybe it just reuses a template
                                        // label?
                                        // Let's map quantity. If it's negative (out), we show as positive 'Out'.
                                        // If it is positive (in), does it show?
                                        // Safest is to show Math.abs() and maybe let user filter in frontend, or assume
                                        // "Qty Out" implies logic.
                                        // Let's just return the quantity. If strictly "Qty Out" report, we might filter
                                        // changeQuantity < 0.
                                        // But search just says "Item:", "From Location", "From", "To".
                                        // I will return Math.abs() for now as it seems most logical for a column "Qty
                                        // Out" (amount leaving stock).
                                        // Cost is unit cost or total value? "Cost" usually unit cost.
                                        Double qty = sm.getChangeQuantity() != null
                                                        ? (double) Math.abs(sm.getChangeQuantity())
                                                        : 0.0;
                                        BigDecimal cost = (sm.getProductVariant() != null)
                                                        ? toBigDecimal(sm.getProductVariant().getCostCents())
                                                        : BigDecimal.ZERO;

                                        return new com.example.multi_tanent.pos.dto.report.ItemMovementReportItemDto(
                                                        stockId,
                                                        name,
                                                        description,
                                                        qty,
                                                        cost,
                                                        sm.getCreatedAt().toLocalDate());
                                })
                                .collect(Collectors.toList());
        }

        public List<com.example.multi_tanent.pos.dto.report.SlowSellingReportItemDto> getSlowSellingReport(
                        LocalDate fromDate, LocalDate toDate, Long storeId, String orderType) {
                Tenant tenant = getCurrentTenant();
                OffsetDateTime start = fromDate.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime();
                OffsetDateTime end = toDate.plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime()
                                .minusNanos(1);

                // Fetch sales inside date range
                List<Sale> sales;
                if (storeId != null) {
                        sales = saleRepository.findByTenantIdAndStoreIdAndInvoiceDateBetween(tenant.getId(), storeId,
                                        start, end);
                } else {
                        sales = saleRepository.findByTenantIdAndInvoiceDateBetween(tenant.getId(), start, end);
                }

                // Filter valid sales
                List<Sale> validSales = sales.stream()
                                .filter(s -> "completed".equalsIgnoreCase(s.getStatus())
                                                || "paid".equalsIgnoreCase(s.getPaymentStatus()))
                                .collect(Collectors.toList());

                // Flatten to items and map to helper object or process in stream
                // Key: Variant ID (or SKU/Name combo if variant ID null)
                // Value: Aggregated stats
                Map<Long, List<com.example.multi_tanent.pos.entity.SaleItem>> verifyMap = validSales.stream()
                                .flatMap(s -> s.getItems().stream())
                                .filter(i -> i.getProductVariant() != null)
                                .collect(Collectors.groupingBy(i -> i.getProductVariant().getId()));

                List<com.example.multi_tanent.pos.dto.report.SlowSellingReportItemDto> result = new ArrayList<>();

                for (Map.Entry<Long, List<com.example.multi_tanent.pos.entity.SaleItem>> entry : verifyMap.entrySet()) {
                        List<com.example.multi_tanent.pos.entity.SaleItem> items = entry.getValue();
                        if (items.isEmpty())
                                continue;

                        com.example.multi_tanent.pos.entity.ProductVariant variant = items.get(0).getProductVariant();

                        long totalQty = items.stream()
                                        .mapToLong(com.example.multi_tanent.pos.entity.SaleItem::getQuantity).sum();
                        BigDecimal totalRev = items.stream().map(i -> toBigDecimal(i.getLineTotalCents()))
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                        long salesCount = items.stream().map(i -> i.getSale().getId()).distinct().count();

                        // Last sale date for this item
                        OffsetDateTime lastSaleDate = items.stream()
                                        .map(i -> i.getSale().getInvoiceDate())
                                        .max(java.util.Comparator.naturalOrder())
                                        .orElse(start);

                        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(lastSaleDate.toLocalDate(), toDate);

                        String name = variant.getProduct().getName();
                        if (variant.getSku() != null) {
                                name += " (" + variant.getSku() + ")";
                        }

                        result.add(new com.example.multi_tanent.pos.dto.report.SlowSellingReportItemDto(
                                        variant.getId().toString(),
                                        name,
                                        totalQty,
                                        totalRev,
                                        salesCount,
                                        daysDiff));
                }

                // Sort by Quantity ASC (Slowest selling first)
                // If sorting by Value, could change comparator
                result.sort(java.util.Comparator.comparingLong(
                                com.example.multi_tanent.pos.dto.report.SlowSellingReportItemDto::getQuantity));

                return result;
        }

        public List<com.example.multi_tanent.pos.dto.report.BestSellingReportItemDto> getBestSellingReport(
                        LocalDate fromDate, LocalDate toDate, Long storeId, String orderType) {
                Tenant tenant = getCurrentTenant();
                OffsetDateTime start = fromDate.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime();
                OffsetDateTime end = toDate.plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime()
                                .minusNanos(1);

                List<Sale> sales;
                if (storeId != null) {
                        sales = saleRepository.findByTenantIdAndStoreIdAndInvoiceDateBetween(tenant.getId(), storeId,
                                        start, end);
                } else {
                        sales = saleRepository.findByTenantIdAndInvoiceDateBetween(tenant.getId(), start, end);
                }

                List<Sale> validSales = sales.stream()
                                .filter(s -> "completed".equalsIgnoreCase(s.getStatus())
                                                || "paid".equalsIgnoreCase(s.getPaymentStatus()))
                                .collect(Collectors.toList());

                Map<Long, List<com.example.multi_tanent.pos.entity.SaleItem>> verifyMap = validSales.stream()
                                .flatMap(s -> s.getItems().stream())
                                .filter(i -> i.getProductVariant() != null)
                                .collect(Collectors.groupingBy(i -> i.getProductVariant().getId()));

                List<com.example.multi_tanent.pos.dto.report.BestSellingReportItemDto> result = new ArrayList<>();

                for (Map.Entry<Long, List<com.example.multi_tanent.pos.entity.SaleItem>> entry : verifyMap.entrySet()) {
                        List<com.example.multi_tanent.pos.entity.SaleItem> items = entry.getValue();
                        if (items.isEmpty())
                                continue;

                        com.example.multi_tanent.pos.entity.ProductVariant variant = items.get(0).getProductVariant();

                        long totalQty = items.stream()
                                        .mapToLong(com.example.multi_tanent.pos.entity.SaleItem::getQuantity).sum();
                        BigDecimal totalRev = items.stream().map(i -> toBigDecimal(i.getLineTotalCents()))
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                        long salesCount = items.stream().map(i -> i.getSale().getId()).distinct().count();

                        OffsetDateTime lastSaleDate = items.stream()
                                        .map(i -> i.getSale().getInvoiceDate())
                                        .max(java.util.Comparator.naturalOrder())
                                        .orElse(start);

                        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(lastSaleDate.toLocalDate(), toDate);

                        String name = variant.getProduct().getName();
                        if (variant.getSku() != null) {
                                name += " (" + variant.getSku() + ")";
                        }

                        result.add(new com.example.multi_tanent.pos.dto.report.BestSellingReportItemDto(
                                        variant.getId().toString(),
                                        name,
                                        totalQty,
                                        totalRev,
                                        salesCount,
                                        daysDiff));
                }

                // Sort by Quantity DESC (Best selling first)
                result.sort(java.util.Comparator.comparingLong(
                                com.example.multi_tanent.pos.dto.report.BestSellingReportItemDto::getQuantity)
                                .reversed());

                return result;
        }

        public List<com.example.multi_tanent.pos.dto.report.ItemSalesReportItemDto> getItemSalesReport(
                        LocalDate fromDate, LocalDate toDate, Long storeId, Long categoryId, String itemName) {
                Tenant tenant = getCurrentTenant();
                OffsetDateTime start = fromDate.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime();
                OffsetDateTime end = toDate.plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime()
                                .minusNanos(1);

                List<Sale> sales;
                if (storeId != null) {
                        sales = saleRepository.findByTenantIdAndStoreIdAndInvoiceDateBetween(tenant.getId(), storeId,
                                        start, end);
                } else {
                        sales = saleRepository.findByTenantIdAndInvoiceDateBetween(tenant.getId(), start, end);
                }

                List<Sale> validSales = sales.stream()
                                .filter(s -> "completed".equalsIgnoreCase(s.getStatus())
                                                || "paid".equalsIgnoreCase(s.getPaymentStatus()))
                                .collect(Collectors.toList());

                // Flatten to filtered items
                List<com.example.multi_tanent.pos.entity.SaleItem> items = validSales.stream()
                                .flatMap(s -> s.getItems().stream())
                                .filter(i -> i.getProductVariant() != null)
                                .filter(i -> categoryId == null
                                                || (i.getProductVariant().getProduct().getCategory() != null
                                                                && i.getProductVariant().getProduct().getCategory()
                                                                                .getId().equals(categoryId)))
                                .filter(i -> itemName == null
                                                || i.getProductVariant().getProduct().getName().toLowerCase()
                                                                .contains(itemName.toLowerCase()))
                                .collect(Collectors.toList());

                // Group by product variant id
                Map<Long, List<com.example.multi_tanent.pos.entity.SaleItem>> groupedItems = items.stream()
                                .collect(Collectors.groupingBy(i -> i.getProductVariant().getId()));

                List<com.example.multi_tanent.pos.dto.report.ItemSalesReportItemDto> result = new ArrayList<>();

                for (Map.Entry<Long, List<com.example.multi_tanent.pos.entity.SaleItem>> entry : groupedItems
                                .entrySet()) {
                        List<com.example.multi_tanent.pos.entity.SaleItem> variantItems = entry.getValue();
                        if (variantItems.isEmpty())
                                continue;

                        com.example.multi_tanent.pos.entity.ProductVariant variant = variantItems.get(0)
                                        .getProductVariant();

                        long totalQty = variantItems.stream()
                                        .mapToLong(com.example.multi_tanent.pos.entity.SaleItem::getQuantity).sum();

                        // Total with discount (Line Total)
                        BigDecimal totalWithDiscount = variantItems.stream()
                                        .map(i -> toBigDecimal(i.getLineTotalCents()))
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                        // Discount Amount
                        BigDecimal discountAmount = variantItems.stream()
                                        .map(i -> i.getDiscountCents() != null ? toBigDecimal(i.getDiscountCents())
                                                        : BigDecimal.ZERO)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                        // Total without discount = Total With Discount + Discount Amount
                        BigDecimal totalWithoutDiscount = totalWithDiscount.add(discountAmount);

                        // Unit Price (Average? Or just take current? Usually average unit price sold
                        // at)
                        // If totalQty is 0 (shouldn't be), avoid division by zero.
                        BigDecimal avgUnitPrice = totalQty > 0
                                        ? totalWithoutDiscount.divide(BigDecimal.valueOf(totalQty),
                                                        java.math.RoundingMode.HALF_UP)
                                        : BigDecimal.ZERO;

                        String name = variant.getProduct().getName();
                        if (variant.getSku() != null) {
                                name += " (" + variant.getSku() + ")";
                        }

                        result.add(new com.example.multi_tanent.pos.dto.report.ItemSalesReportItemDto(
                                        variant.getId().toString(),
                                        name,
                                        totalQty,
                                        avgUnitPrice,
                                        totalWithoutDiscount,
                                        discountAmount,
                                        totalWithDiscount));
                }

                // Sort by name
                result.sort(java.util.Comparator.comparing(
                                com.example.multi_tanent.pos.dto.report.ItemSalesReportItemDto::getItemName));

                return result;
        }
}
