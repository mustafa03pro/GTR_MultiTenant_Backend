package com.example.multi_tanent.sales.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RentalItemRecievedItemRequest {
    private Long id; // for updates
    private Long crmProductId;
    private String itemName;
    private String itemCode;
    private String description;
    private Integer doQuantity;
    private Integer receivedQuantity;
    private Integer currentReceiveQuantity;
    private Integer remainingQuantity;
}
