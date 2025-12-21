package com.example.multi_tanent.purchases.dto;

// package com.example.multi_tanent.purchase.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurPurchaseOrderResponse {
    private Long id;

    private String orderCategory;
    private Long supplierId;
    private String supplierName;

    private String poNumber;
    private String reference;
    private LocalDate date;

    private String discountMode;
    private String currency;
    private String remark;
    private String status;

    private String createdBy;
    private LocalDateTime createdAt;

    private BigDecimal subTotal;
    private BigDecimal totalDiscount;
    private BigDecimal totalTax;
    private BigDecimal otherCharges;
    private BigDecimal totalAmount;
    private BigDecimal grossTotal;

    private List<PurPurchaseOrderItemResponse> items;
    private List<PurPurchaseOrderAttachmentResponse> attachments;
    private Long tenantId;

    private Long locationId;
    private String locationName;
    private String projectNumber;
    private String deliverToType;
    private Long deliverToId;
    private String deliverToAddress;
    private String template;
    private String emailTo;
    private String termsAndConditions;
    private String notes;
}
