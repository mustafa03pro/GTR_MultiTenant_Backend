package com.example.multi_tanent.sales.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecievedRequest {

    private String entryType;
    private Long locationId;
    private Long customerId;
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
}
