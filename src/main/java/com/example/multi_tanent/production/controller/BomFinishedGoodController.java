package com.example.multi_tanent.production.controller;

import com.example.multi_tanent.production.dto.BomFinishedGoodRequest;
import com.example.multi_tanent.production.dto.BomFinishedGoodResponse;
import com.example.multi_tanent.production.services.BomFinishedGoodService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;

@RestController
@RequestMapping("/api/production/bom-finished-goods")
@RequiredArgsConstructor
public class BomFinishedGoodController {

    private final BomFinishedGoodService service;

    @PostMapping
    public ResponseEntity<BomFinishedGoodResponse> create(@RequestBody BomFinishedGoodRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BomFinishedGoodResponse> update(@PathVariable Long id,
            @RequestBody BomFinishedGoodRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BomFinishedGoodResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<BomFinishedGoodResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(service.getAll(pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<InputStreamResource> exportToExcel(@PathVariable Long id) {
        ByteArrayInputStream in = service.exportToExcel(id);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=bom_finished_good_" + id + ".xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }
}
