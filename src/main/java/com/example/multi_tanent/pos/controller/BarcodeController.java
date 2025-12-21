package com.example.multi_tanent.pos.controller;

import com.example.multi_tanent.pos.entity.ProductVariant;
import com.example.multi_tanent.pos.repository.ProductVariantRepository;
import com.example.multi_tanent.pos.service.BarCodeService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pos/barcodes")
@CrossOrigin(origins = "*")
public class BarcodeController {

    private final BarCodeService barCodeService;
    private final ProductVariantRepository productVariantRepository;

    public BarcodeController(BarCodeService barCodeService, ProductVariantRepository productVariantRepository) {
        this.barCodeService = barCodeService;
        this.productVariantRepository = productVariantRepository;
    }

    @GetMapping(value = "/variant/{variantId}", produces = MediaType.IMAGE_JPEG_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> generateBarcodeForVariant(
            @PathVariable Long variantId,
            @RequestParam(defaultValue = "250") int width,
            @RequestParam(defaultValue = "150") int height) {

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("ProductVariant not found with id: " + variantId));

        try {
            String barcodeText = variant.getBarcode();
            if (barcodeText == null || barcodeText.isEmpty()) {
                return ResponseEntity.badRequest().body(null);
            }
            byte[] barcode = barCodeService.generateBarcodeImage(barcodeText, width, height);
            return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(barcode);
        } catch (Exception e) {
            // Log the exception
            return ResponseEntity.internalServerError().build();
        }
    }
}
