package com.example.multi_tanent.pos.dto;

import com.example.multi_tanent.config.deserializer.StringObjectDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentRequest {
    @NotBlank(message = "Payment method is required.")
    @JsonDeserialize(using = StringObjectDeserializer.class)
    private String method; // e.g., cash, card

    @Min(value = 0, message = "Amount cannot be negative.")
    private Long amountCents;

    @JsonDeserialize(using = StringObjectDeserializer.class)
    private String reference; // e.g., transaction ID
}