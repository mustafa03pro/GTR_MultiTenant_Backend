package com.example.multi_tanent.pos.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentChangeReportItemDto {
    private String orderNo;
    private String transNo; // payment Id or Ref
    private String user;
    private String cardAssignedUser;
    private String branch;
    private String fromAccount;
    private String toAccount;
    private LocalDate date;
}
