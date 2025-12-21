package com.example.multi_tanent.pos.dto.report;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessSummaryDto {

    private SalesSummary salesSummary;
    private List<OrderTypeSummary> orderTypes;
    private List<SalesSourceSummary> salesSources;
    private List<GuestCountSummary> guestCounts;

    private CostProfitSummary costProfit;
    private List<CollectionsSummary> collections;
    private List<TaxReportSummary> taxReports;
    private List<StaffReportSummary> staffReports;
    private List<CategoryReportSummary> categoryReports;
    private WastageSummary wastage;

    @Data
    @AllArgsConstructor
    public static class CostProfitSummary {
        private BigDecimal cogs; // Cost of Goods Sold
        private BigDecimal wastage;
        private BigDecimal grossProfit;
    }

    @Data
    @AllArgsConstructor
    public static class CollectionsSummary {
        private String method; // Account e.g. Cash, Card
        private Long quantity;
        private BigDecimal amount;
    }

    @Data
    @AllArgsConstructor
    public static class TaxReportSummary {
        private String rateName;
        private BigDecimal ratePercent;
        private BigDecimal salesAmount; // Total Sales Amount attracting this tax
        private BigDecimal taxAmount;
    }

    @Data
    @AllArgsConstructor
    public static class StaffReportSummary {
        private String staffName;
        private Long quantity;
        private BigDecimal amount;
    }

    @Data
    @AllArgsConstructor
    public static class CategoryReportSummary {
        private String categoryName;
        private Long quantity;
        private BigDecimal amount;
    }

    @Data
    @AllArgsConstructor
    public static class WastageSummary {
        private BigDecimal totalWastageCost;
    }

    @Data
    @AllArgsConstructor
    public static class SalesSummary {
        private BigDecimal sales; // Subtotal
        private BigDecimal deliveryCharge;
        private BigDecimal paidModifiers; // Assuming 0 for now as no field exists
        private BigDecimal grossSales; // Subtotal + Delivery + Modifiers
        private BigDecimal discounts;
        private BigDecimal netSalesIncludingVat; // Gross - Discount
        private BigDecimal vat;
        private BigDecimal netSalesExcludingVat; // NetIncludingVat - VAT
        private BigDecimal fnbSalesMinusVat; // Same as NetExcludingVat for now
    }

    @Data
    @AllArgsConstructor
    public static class OrderTypeSummary {
        private String orderType;
        private Long ordersCount; // "Orders" column
        private BigDecimal value;
    }

    @Data
    @AllArgsConstructor
    public static class SalesSourceSummary {
        private String salesSource;
        private Long quantity; // "Qty" column
        private BigDecimal amount;
    }

    @Data
    @AllArgsConstructor
    public static class GuestCountSummary {
        private String description;
        private Long count;
    }
}
