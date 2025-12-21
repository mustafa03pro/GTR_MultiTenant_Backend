package com.example.multi_tanent.sales.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class DeliveryOrderRequest {
    private LocalDate deliveryOrderDate;
    private LocalDate shipmentDate;
    private Long customerId;
    private Long salesOrderId;
    private String reference;
    private String poNumber;
    private Long salespersonId;
    private List<DeliveryOrderItemRequest> items;
    private BigDecimal totalDiscount;
    private BigDecimal otherCharges;
    private String termsAndConditions;
    private String notes;
    private String emailTo;
    private String template;
    private String status;
}
