package com.example.multi_tanent.pos.controller;

import com.example.multi_tanent.pos.dto.ProductDto;
import com.example.multi_tanent.pos.dto.ProductRequest;
import com.example.multi_tanent.pos.entity.Product;
import com.example.multi_tanent.pos.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pos/products")
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','POS_ADMIN', 'POS_MANAGER')")
    public ResponseEntity<Product> createProduct(@Valid @RequestBody ProductRequest productRequest) { // Keeping Product
                                                                                                      // for create
                                                                                                      // response
                                                                                                      // consistency
        Product createdProduct = productService.createProduct(productRequest);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdProduct.getId())
                .toUri();
        return ResponseEntity.created(location).body(createdProduct);
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','POS_ADMIN', 'POS_MANAGER')")
    public ResponseEntity<?> bulkAddProducts(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Please select a file to upload."));
        }

        try {
            String result = productService.bulkAddProducts(file);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/bulk-template")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','POS_ADMIN', 'POS_MANAGER')")
    public ResponseEntity<byte[]> downloadBulkAddTemplate() {
        try {
            byte[] excelContent = productService.generateBulkAddTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(
                    MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "product_bulk_upload_template.xlsx");
            return ResponseEntity.ok().headers(headers).body(excelContent);
        } catch (IOException e) {
            // In a real app, you'd want to log this exception
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProductsForCurrentTenant());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/category/{categoryName}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProductDto>> getProductsByCategoryName(@PathVariable String categoryName) {
        return ResponseEntity.ok(productService.getProductsByCategoryName(categoryName));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','POS_ADMIN', 'POS_MANAGER')")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable Long id,
            @Valid @RequestBody ProductRequest productRequest) {
        return ResponseEntity.ok(productService.updateProduct(id, productRequest));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','POS_ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/hard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','POS_ADMIN')")
    public ResponseEntity<Void> hardDeleteProduct(@PathVariable Long id) {
        productService.hardDeleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
