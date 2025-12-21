package com.example.multi_tanent.sales.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class FollowUpRequest {
    private Long quotationId;
    private Long rentalQuotationId;
    private LocalDate nextFollowupDate;
    private LocalTime nextFollowupTime;
    private String quotationStatus;
    private String comment;
    private Long employeeId;
}
