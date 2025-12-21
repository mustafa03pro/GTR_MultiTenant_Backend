package com.example.multi_tanent.sales.controller;

import com.example.multi_tanent.sales.dto.RecievedRequest;
import com.example.multi_tanent.sales.dto.RecievedResponse;
import com.example.multi_tanent.sales.service.RecievedService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recieved")
@RequiredArgsConstructor
public class RecievedController {

    private final RecievedService recievedService;

    @GetMapping
    public ResponseEntity<List<RecievedResponse>> getAll(HttpServletRequest request) {
        return ResponseEntity.ok(recievedService.getAll(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecievedResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(recievedService.getById(id));
    }

    @PostMapping
    public ResponseEntity<RecievedResponse> create(@RequestBody RecievedRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recievedService.create(request, httpRequest));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecievedResponse> update(@PathVariable Long id, @RequestBody RecievedRequest request) {
        return ResponseEntity.ok(recievedService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        recievedService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
