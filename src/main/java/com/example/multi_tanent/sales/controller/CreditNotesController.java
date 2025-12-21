package com.example.multi_tanent.sales.controller;

import com.example.multi_tanent.sales.dto.CreditNotesRequest;
import com.example.multi_tanent.sales.dto.CreditNotesResponse;
import com.example.multi_tanent.sales.service.CreditNotesService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/credit-notes")
@RequiredArgsConstructor
public class CreditNotesController {

    private final CreditNotesService creditNotesService;

    @GetMapping
    public ResponseEntity<List<CreditNotesResponse>> getAll(HttpServletRequest request) {
        return ResponseEntity.ok(creditNotesService.getAll(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CreditNotesResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(creditNotesService.getById(id));
    }

    @PostMapping
    public ResponseEntity<CreditNotesResponse> create(@RequestBody CreditNotesRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(creditNotesService.create(request, httpRequest));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CreditNotesResponse> update(@PathVariable Long id, @RequestBody CreditNotesRequest request) {
        return ResponseEntity.ok(creditNotesService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        creditNotesService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
