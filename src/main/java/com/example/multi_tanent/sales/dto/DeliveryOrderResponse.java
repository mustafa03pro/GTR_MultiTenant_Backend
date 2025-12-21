package com.example.multi_tanent.sales.dto;

import com.example.multi_tanent.sales.enums.SalesStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class DeliveryOrderResponse {
    private Long id;
    private LocalDate deliveryOrderDate;
    private LocalDate shipmentDate;
    private Long customerId;
    private String customerName;
    private Long salesOrderId;
    private String salesOrderNumber;
    private String deliveryOrderNumber;
    private String reference;
    private String poNumber;
    private Long salespersonId;
    private String salespersonName;
    private List<DeliveryOrderItemResponse> items;
    private BigDecimal subTotal;
    private BigDecimal totalDiscount;
    private BigDecimal grossTotal;
    private BigDecimal totalTax;
    private BigDecimal otherCharges;
    private BigDecimal netTotal;
    private String termsAndConditions;
    private String notes;
    private List<String> attachments;
    private String emailTo;
    private String template;
    private SalesStatus status;
    private String createdBy;
    private String updatedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
