package com.example.multi_tanent.sales.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecievedResponse {

    private Long id;
    private String entryType;
    private Long locationId;
    private String locationName;
    private Long customerId;
    private String customerName;
    private String piNumber;
    private String manualPiNumber;
    private BigDecimal amount;
    private Boolean isReceivedFullAmount;
    private Boolean isTaxDeducted;
    private LocalDate depositDate;
    private String depositMode;
    private String reference;
    private String chequeNumber;
    private LocalDate receivingDate;
    private String receivedNumber;
    private String invoiceNumber;
    private BigDecimal tds;
    private BigDecimal advanceAmount;
    private BigDecimal totalPiAmount;
    private BigDecimal fbc;
    private BigDecimal expectedInFc;
    private BigDecimal bankCharges;
    private BigDecimal fineAndPenalty;
    private BigDecimal rebateAndDiscount;
    private String createdBy;
    private String updatedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
