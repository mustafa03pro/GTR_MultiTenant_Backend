package com.example.multi_tanent.pos.dto.report;

import lombok.Data;

@Data
public class UserTransferRequestDto {
    private Long userId;
    private Long fromStoreId;
    private Long toStoreId;
}
