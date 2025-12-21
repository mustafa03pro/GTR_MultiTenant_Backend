// package com.example.multi_tanent.purchases.controller;

// import jakarta.validation.Valid;
// import lombok.RequiredArgsConstructor;
// import org.springframework.data.domain.*;
// import org.springframework.http.*;
// import org.springframework.web.bind.annotation.*;

// import com.example.multi_tanent.purchases.dto.PurPurchaseOrderRequest;
// import com.example.multi_tanent.purchases.dto.PurPurchaseOrderResponse;
// import com.example.multi_tanent.purchases.service.PurPurchaseOrderService;

// @RestController
// @RequestMapping("/api/purchase/orders")
// @RequiredArgsConstructor
// @CrossOrigin(origins = "*")
// public class PurPurchaseOrderController {

//     private final PurPurchaseOrderService service;

//     @PostMapping
//     public ResponseEntity<PurPurchaseOrderResponse> create(@Valid @RequestBody PurPurchaseOrderRequest req) {
//         PurPurchaseOrderResponse resp = service.create(req);
//         return ResponseEntity.status(HttpStatus.CREATED).body(resp);
//     }

//     @PostMapping("/{id}/convert-to-bill")
//     public ResponseEntity<PurPurchaseOrderResponse> convertToBill(@PathVariable Long id) {
//         PurPurchaseOrderResponse resp = service.convertToBill(id);
//         return ResponseEntity.ok(resp);
//     }

//     @GetMapping
//     public ResponseEntity<Page<PurPurchaseOrderResponse>> list(
//             @RequestParam(defaultValue = "0") int page,
//             @RequestParam(defaultValue = "20") int size,
//             @RequestParam(defaultValue = "createdAt,desc") String sort // e.g. "createdAt,desc"
//     ) {
//         Sort s = Sort.by(Sort.Direction.DESC, "createdAt");
//         try {
//             String[] sp = sort.split(",");
//             if (sp.length == 2) {
//                 s = Sort.by(Sort.Direction.fromString(sp[1]), sp[0]);
//             }
//         } catch (Exception ignored) {
//         }
//         Pageable p = PageRequest.of(page, size, s);
//         return ResponseEntity.ok(service.list(p));
//     }

//     @GetMapping("/{id}")
//     public ResponseEntity<PurPurchaseOrderResponse> getById(@PathVariable Long id) {
//         PurPurchaseOrderResponse resp = service.getById(id);
//         return ResponseEntity.ok(resp);
//     }

//     @PutMapping("/{id}")
//     public ResponseEntity<PurPurchaseOrderResponse> update(@PathVariable Long id,
//             @Valid @RequestBody PurPurchaseOrderRequest req) {
//         PurPurchaseOrderResponse resp = service.update(id, req);
//         return ResponseEntity.ok(resp);
//     }

//     @DeleteMapping("/{id}")
//     public ResponseEntity<Void> delete(@PathVariable Long id) {
//         service.delete(id);
//         return ResponseEntity.noContent().build();
//     }
// }

package com.example.multi_tanent.purchases.controller;

import com.example.multi_tanent.purchases.dto.*;
import com.example.multi_tanent.purchases.service.PurPurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/purchase/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PurPurchaseOrderController {

    private final PurPurchaseOrderService service;

    @PostMapping
    public ResponseEntity<PurPurchaseOrderResponse> create(@RequestBody PurPurchaseOrderRequest req) {
        PurPurchaseOrderResponse resp = service.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping
    public ResponseEntity<Page<PurPurchaseOrderResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        Sort s = Sort.by(Sort.Direction.DESC, "createdAt");
        try {
            String[] sp = sort.split(",");
            if (sp.length == 2)
                s = Sort.by(Sort.Direction.fromString(sp[1]), sp[0]);
        } catch (Exception ignored) {
        }
        Pageable p = PageRequest.of(page, size, s);
        return ResponseEntity.ok(service.list(p));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurPurchaseOrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PurPurchaseOrderResponse> update(@PathVariable Long id,
            @RequestBody PurPurchaseOrderRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/convert-to-bill")
    public ResponseEntity<PurPurchaseInvoiceResponse> convertToBill(@PathVariable Long id) {
        return ResponseEntity.ok(service.convertToBill(id));
    }

    @PostMapping("/{id}/convert-to-payment")
    public ResponseEntity<PurPurchasePaymentResponse> convertToPayment(@PathVariable Long id) {
        return ResponseEntity.ok(service.convertToPayment(id));
    }

    /**
     * Upload attachments for an existing purchase order.
     * Returns list of public URLs for uploaded files.
     */
    @PostMapping("/{id}/attachments")
    public ResponseEntity<Map<String, List<String>>> uploadAttachments(
            @PathVariable Long id,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "uploadedBy", required = false) String uploadedBy) {

        List<String> urls = service.attachFiles(id, files, uploadedBy);
        return ResponseEntity.ok(Map.of("files", urls));
    }
}
