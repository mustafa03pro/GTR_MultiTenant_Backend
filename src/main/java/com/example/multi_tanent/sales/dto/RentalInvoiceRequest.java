package com.example.multi_tanent.sales.dto;

import com.example.multi_tanent.sales.enums.InvoiceType;
import com.example.multi_tanent.sales.enums.SalesStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class RentalInvoiceRequest {
    private String invoiceLedger;
    private LocalDate invoiceDate;
    private Long customerId;
    private String customerName; // For manual entry if needed, though usually ID is preferred
    private String doNumber;
    private String lpoNumber;
    private LocalDate requiredDate;
    private LocalDate dueDate;
    private Long salespersonId;
    private String poNumber;
    private String reference;
    private InvoiceType invoiceType;
    private Boolean enableGrossNetWeight;
    private List<RentalInvoiceItemRequest> items;
    private BigDecimal subTotal;
    private BigDecimal totalDiscount;
    private BigDecimal grossTotal;
    private BigDecimal totalTax;
    private BigDecimal otherCharges;
    private BigDecimal netTotal;
    private SalesStatus status;
    private String termsAndConditions;
    private String notes;
    private String template;
    private String emailTo;
}
