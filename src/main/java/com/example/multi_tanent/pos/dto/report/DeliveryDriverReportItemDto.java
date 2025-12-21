package com.example.multi_tanent.pos.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryDriverReportItemDto {
    private String orderNo;
    private String status;
    private LocalDate orderDate;
    private LocalTime dispatchedTime;
    private LocalTime deliveredTime;
    private String ridingTime;
    private BigDecimal totalAmount;
    private String paymentType;
    private String address;
    private LocalTime expectedDeliveryTime;
}
