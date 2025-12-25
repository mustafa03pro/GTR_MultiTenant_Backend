package com.example.multi_tanent.production.dto;

import com.example.multi_tanent.production.enums.ManufacturingOrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ManufacturingOrderRequest {

    private Long productionHouseId; // productionHouse

    @NotNull(message = "MO Number is required")
    private String moNumber;

    private Long salesOrderId;

    private Long customerId;

    private String referenceNo;

    @NotNull(message = "Item ID is required")
    private Long itemId;

    private BigDecimal quantity;

    private ManufacturingOrderStatus status;

    private OffsetDateTime scheduleStart;
    private OffsetDateTime scheduleFinish;
    private OffsetDateTime dueDate;
    private OffsetDateTime actualStart;
    private OffsetDateTime actualFinish;

    private Long assignToId;

    private String batchNo;

    private String samplingRequestStatus;

    private Long bomId;
}
