package com.example.multi_tanent.pos.dto.report;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class TotalSalesReportItemDto {
    private String ref;
    private String orderType;
    private String location;
    private String customer;
    private String debtor; // Can be customer name if unpaid, or empty
    private String salesSource;
    private String salesSourceReference;
    private LocalDate orderDate;
    private LocalTime orderTime;
    private BigDecimal subTotal;
    private BigDecimal discount;
    private BigDecimal vat;
    private BigDecimal shippingCharge;
    private BigDecimal orderTotal;
    private BigDecimal customerPaidAmount;
    private BigDecimal returnedAmount;
    private String paymentMethod;
    private LocalTime settlementTime;
}
