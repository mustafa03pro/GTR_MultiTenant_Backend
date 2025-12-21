package com.example.multi_tanent.pos.dto.report;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailySalesSummaryDto {

    private String date;
    private String time;
    private String storeName; // "Service 4 U" in image

    private CashReportSection cashReport;
    private List<TaxReportItem> taxReports;
    private PosReportSection posReport;
    private List<CancellationReportItem> cancellationReports;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CashReportSection {
        private SalesSection sales;
        private CollectionSection collection;
        private OthersSection others;
        private DiscountDetailsSection discountDetails;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SalesSection {
        private Long subTotalQty;
        private BigDecimal subTotalAmount;

        private BigDecimal discount;
        private BigDecimal shippingCharge;
        private BigDecimal vat;
        private BigDecimal billAmount;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CollectionSection {
        private BigDecimal netCollection;
        private BigDecimal tipAmount;
        private BigDecimal onAccount;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OthersSection {
        private Long vatQty;
        private BigDecimal vatAmount;
        private Long deliveryChargesQty;
        private BigDecimal deliveryChargesAmount;
        private BigDecimal total;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DiscountDetailsSection {
        private BigDecimal total;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TaxReportItem {
        private String rate; // e.g. "Vat 5% (5.00%)"
        private BigDecimal totalSalesAmount;
        private BigDecimal taxAmount;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PosReportSection {
        private BigDecimal grossSales;
        private BigDecimal deduction;
        private BigDecimal netSale;
        private BigDecimal total;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CancellationReportItem {
        private String code;
        private String name;
        private Long quantity;
        private BigDecimal amount;
    }
}
