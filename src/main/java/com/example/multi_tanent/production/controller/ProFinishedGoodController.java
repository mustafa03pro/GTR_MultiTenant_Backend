package com.example.multi_tanent.production.controller;

import com.example.multi_tanent.production.dto.ProFinishedGoodRequest;
import com.example.multi_tanent.production.dto.ProFinishedGoodResponse;
import com.example.multi_tanent.production.services.ProFinishedGoodService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;

@RestController
@RequestMapping("/api/production/finished-goods")
@RequiredArgsConstructor
public class ProFinishedGoodController {

    private final ProFinishedGoodService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProFinishedGoodResponse> createFinishedGood(
            @ModelAttribute ProFinishedGoodRequest request) {
        return ResponseEntity.ok(service.createFinishedGood(request));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProFinishedGoodResponse> updateFinishedGood(
            @PathVariable Long id,
            @ModelAttribute ProFinishedGoodRequest request) {
        return ResponseEntity.ok(service.updateFinishedGood(id, request));
    }

    @GetMapping
    public ResponseEntity<List<ProFinishedGoodResponse>> getAllFinishedGoods() {
        return ResponseEntity.ok(service.getAllFinishedGoods());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProFinishedGoodResponse> getFinishedGoodById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getFinishedGoodById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFinishedGood(@PathVariable Long id) {
        service.deleteFinishedGood(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importFromExcel(@RequestParam("file") MultipartFile file) {
        service.importFromExcel(file);
        return ResponseEntity.ok("Import processed successfully.");
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<org.springframework.core.io.Resource> getFinishedGoodImage(@PathVariable Long id) {
        org.springframework.core.io.Resource resource = service.getFinishedGoodImage(id);

        String contentType = "application/octet-stream";
        try {
            contentType = java.nio.file.Files.probeContentType(resource.getFile().toPath());
        } catch (Exception ex) {
            // fallback
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/{id}/barcode-image")
    public ResponseEntity<org.springframework.core.io.Resource> getFinishedGoodBarcodeImage(@PathVariable Long id) {
        org.springframework.core.io.Resource resource = service.getFinishedGoodBarcodeImage(id);

        String contentType = "image/png"; // Barcodes are usually PNGs from our service
        try {
            contentType = java.nio.file.Files.probeContentType(resource.getFile().toPath());
        } catch (Exception ex) {
            // fallback
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/view-file")
    public ResponseEntity<org.springframework.core.io.Resource> viewFile(@RequestParam String path) {
        org.springframework.core.io.Resource resource = service.loadFile(path);
        String contentType = "application/octet-stream";
        try {
            contentType = java.nio.file.Files
                    .probeContentType(java.nio.file.Paths.get(resource.getFile().getAbsolutePath()));
        } catch (Exception ex) {
            // fallback
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> exportToExcel() {
        ByteArrayInputStream in = service.exportToExcel();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=finished_goods_export.xlsx");
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }

    @GetMapping("/template")
    public ResponseEntity<InputStreamResource> downloadTemplate() {
        ByteArrayInputStream in = service.downloadTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=finished_goods_import_template.xlsx");
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }
}
