package com.example.multi_tanent.sales.dto;

import com.example.multi_tanent.sales.enums.SalesStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
public class PaymentSheduleResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private Long rentalSalesOrderId;
    private String rentalSalesOrderNumber;
    private LocalDate dueDate;
    private BigDecimal amount;
    private SalesStatus status;
    private String note;
    private String createdBy;
    private String updatedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
