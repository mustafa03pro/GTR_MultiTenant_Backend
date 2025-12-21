package com.example.multi_tanent.sales.dto;

import com.example.multi_tanent.sales.enums.SalesStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
public class RentalItemRecievedResponse {
    private Long id;
    private Long tenantId;
    private LocalDate doDate;

    private Long customerId;
    private String customerName;

    private String billingAddress;
    private String shippingAddress;

    private String doNumber;
    private String orderNumber;

    private Long rentalSalesOrderId;

    private List<RentalItemRecievedItemResponse> items;

    private SalesStatus status;

    private String createdBy;
    private OffsetDateTime createdAt;
}
