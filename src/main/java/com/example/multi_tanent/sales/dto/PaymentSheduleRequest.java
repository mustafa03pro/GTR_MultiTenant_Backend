package com.example.multi_tanent.sales.dto;

import com.example.multi_tanent.sales.enums.SalesStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PaymentSheduleRequest {
    private Long customerId;
    private Long rentalSalesOrderId;
    private LocalDate dueDate;
    private BigDecimal amount;
    private SalesStatus status;
    private String note;
}
