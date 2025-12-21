package com.example.multi_tanent.sales.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditNotesRequest {

    private Long locationId;
    private Long customerId;
    private String creditNoteNumber; // Can be manual or auto-generated
    private String invoiceNumber;
    private LocalDate creditNoteDate;
    private BigDecimal amount;
    private BigDecimal taxPercentage;
    private String termsAndConditions;
    private String notes;
    private String template;
    private String emailTo; // Can be comma separated emails
    private List<String> attachments;
}
