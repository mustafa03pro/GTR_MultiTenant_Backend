package com.example.multi_tanent.pos.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesSupportReportItemDto {
    private String ref;
    private String orderType;
    private String customer;
    private String orderTakenBy;
    private LocalDate orderDate;
    private LocalTime orderTime;
    private String salesSource;
    private String salesSourceReference;
    private BigDecimal subTotal;
    private BigDecimal discount;
    private BigDecimal vat;
    private BigDecimal shippingCharge;
    private BigDecimal orderTotal;
    private BigDecimal customerPaidAmount;
    private BigDecimal returnedAmount;
    private String paymentMethod;
    private String billReceivedBy;
}
