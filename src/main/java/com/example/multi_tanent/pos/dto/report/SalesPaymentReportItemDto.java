package com.example.multi_tanent.pos.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesPaymentReportItemDto {
    private String type;
    private String orderRef;
    private LocalDate orderDate;
    private String reference;
    private String customer;
    private LocalDate paymentDate;
    private BigDecimal customerPaidAmount;
    private BigDecimal amount;
    private BigDecimal customerReturnedAmount;
    private String currency;
    private String bankName;
    private String newPayMethod;
    private String cardNumber;
}
