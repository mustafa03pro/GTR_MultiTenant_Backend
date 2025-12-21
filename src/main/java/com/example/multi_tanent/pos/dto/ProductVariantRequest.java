package com.example.multi_tanent.pos.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProductVariantRequest {
    private String sku;

    private String barcode;
    private JsonNode attributes;
    private long priceCents;
    private long costCents;
    private String imageUrl;
    private Long taxRateId;
    private Boolean active;
}