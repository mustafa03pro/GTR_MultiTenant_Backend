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
public class CancellationReportItemDto {
    private String billNo; // Invoice No
    private String ref; // Order ID or Reference
    private String orderType;
    private String pos; // Store Name
    private String orderTakenBy; // User Name
    private String cardUser; // Optional, User who processed card or same as order user
    private LocalDate date;
    private String reason;
    private BigDecimal amount;
    private LocalTime orderPlacedTime;
    private LocalTime orderCancelledTime;
    private String paymentMethod;
}
