package com.example.multi_tanent.sales.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RentalItemRecievedItemResponse {
    private Long id;
    private Long crmProductId;
    private String itemName;
    private String itemCode;
    private String description;
    private Integer doQuantity;
    private Integer receivedQuantity;
    private Integer currentReceiveQuantity;
    private Integer remainingQuantity;
}
