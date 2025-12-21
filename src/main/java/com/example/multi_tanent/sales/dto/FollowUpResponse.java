package com.example.multi_tanent.sales.dto;

import com.example.multi_tanent.sales.enums.SalesStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Data
@Builder
public class FollowUpResponse {
    private Long id;
    private Long tenantId;
    private Long quotationId;
    private String quotationNumber;
    private Long rentalQuotationId;
    private String rentalQuotationNumber;
    private LocalDate nextFollowupDate;
    private LocalTime nextFollowupTime;
    private SalesStatus quotationStatus;
    private String comment;
    private Long employeeId;
    private String employeeName;
    private String createdBy;
    private OffsetDateTime createdAt;
}
