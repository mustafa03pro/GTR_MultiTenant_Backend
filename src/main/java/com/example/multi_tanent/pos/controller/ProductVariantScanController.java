package com.example.multi_tanent.pos.controller;

import com.example.multi_tanent.pos.dto.ProductVariantDto;
import com.example.multi_tanent.pos.service.ProductVariantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pos/product-variants")
@CrossOrigin(origins = "*")
public class ProductVariantScanController {

    private final ProductVariantService productVariantService;

    public ProductVariantScanController(ProductVariantService productVariantService) {
        this.productVariantService = productVariantService;
    }

    @GetMapping("/barcode/{barcode}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductVariantDto> getVariantByBarcode(@PathVariable String barcode) {
        return productVariantService.getVariantByBarcode(barcode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
