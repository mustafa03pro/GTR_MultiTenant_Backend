package com.example.multi_tanent.production.controller;

import com.example.multi_tanent.production.dto.ProcessFinishedGoodRequest;
import com.example.multi_tanent.production.dto.ProcessFinishedGoodResponse;
import com.example.multi_tanent.production.services.ProcessFinishedGoodService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/production/process-finished-goods")
@RequiredArgsConstructor
public class ProcessFinishedGoodController {

    private final ProcessFinishedGoodService service;

    @PostMapping
    public ResponseEntity<ProcessFinishedGoodResponse> create(@RequestBody ProcessFinishedGoodRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProcessFinishedGoodResponse> update(@PathVariable Long id,
            @RequestBody ProcessFinishedGoodRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProcessFinishedGoodResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<ProcessFinishedGoodResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(service.getAll(pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
