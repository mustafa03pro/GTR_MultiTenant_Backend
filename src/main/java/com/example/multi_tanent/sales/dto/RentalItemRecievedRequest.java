package com.example.multi_tanent.sales.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
public class RentalItemRecievedRequest {
    private LocalDate doDate;
    private Long customerId;
    private String billingAddress;
    private String shippingAddress;
    private String doNumber;
    private String orderNumber;
    private Long rentalSalesOrderId;
    private List<RentalItemRecievedItemRequest> items;
    private String status; // String from FE, mapped to Enum in service
}
