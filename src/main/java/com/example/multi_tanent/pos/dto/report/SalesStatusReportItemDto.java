package com.example.multi_tanent.pos.dto.report;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class SalesStatusReportItemDto {
    private String orderNo;
    private String carNo;
    private String status;
    private LocalDate orderDate;
    private LocalTime deliveredTime;
    private BigDecimal totalAmount;
    private String paymentType;
}
