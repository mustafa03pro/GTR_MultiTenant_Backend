package com.example.multi_tanent.sales.dto;

import com.example.multi_tanent.sales.enums.CreditNoteStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditNotesResponse {

    private Long id;
    private Long locationId;
    private String locationName;
    private Long customerId;
    private String customerName;
    private String creditNoteNumber;
    private String invoiceNumber;
    private LocalDate creditNoteDate;
    private BigDecimal amount;
    private BigDecimal taxPercentage;
    private String termsAndConditions;
    private String notes;
    private String template;
    private String emailTo;
    private CreditNoteStatus status;
    private BigDecimal balanceDue;
    private List<String> attachments;
    private String createdBy;
    private String updatedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
