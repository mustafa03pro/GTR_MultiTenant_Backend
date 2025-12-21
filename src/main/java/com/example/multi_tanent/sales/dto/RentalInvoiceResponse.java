package com.example.multi_tanent.sales.dto;

import com.example.multi_tanent.sales.enums.InvoiceType;
import com.example.multi_tanent.sales.enums.SalesStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class RentalInvoiceResponse {
    private Long id;
    private String invoiceLedger;
    private LocalDate invoiceDate;
    private Long customerId;
    private String customerName;
    private String invoiceNumber;
    private String doNumber;
    private String lpoNumber;
    private LocalDate requiredDate;
    private LocalDate dueDate;
    private Long salespersonId;
    private String salespersonName;
    private String poNumber;
    private String reference;
    private InvoiceType invoiceType;
    private Boolean enableGrossNetWeight;
    private List<RentalInvoiceItemResponse> items;
    private BigDecimal subTotal;
    private BigDecimal totalDiscount;
    private BigDecimal grossTotal;
    private BigDecimal totalTax;
    private BigDecimal otherCharges;
    private BigDecimal netTotal;
    private SalesStatus status;
    private String termsAndConditions;
    private String notes;
    private List<String> attachments;
    private String template;
    private String emailTo;

    // Auditing fields
    private String createdBy;
    private java.time.OffsetDateTime createdAt;
}
