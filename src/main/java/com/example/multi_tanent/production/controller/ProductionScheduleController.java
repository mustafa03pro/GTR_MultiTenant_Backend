package com.example.multi_tanent.production.controller;

import com.example.multi_tanent.production.dto.ProductionScheduleRequest;
import com.example.multi_tanent.production.entity.ProductionSchedule;
import com.example.multi_tanent.production.services.ProductionScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/production-schedules")
@CrossOrigin(origins = "*")
public class ProductionScheduleController {

    private final ProductionScheduleService scheduleService;

    public ProductionScheduleController(ProductionScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping
    public ResponseEntity<ProductionSchedule> createSchedule(@RequestBody ProductionScheduleRequest request) {
        ProductionSchedule created = scheduleService.createSchedule(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductionSchedule> updateSchedule(@PathVariable Long id,
            @RequestBody ProductionScheduleRequest request) {
        return ResponseEntity.ok(scheduleService.updateSchedule(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<ProductionSchedule>> getSchedules(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end,
            @RequestParam(value = "workGroupId", required = false) Long workGroupId,
            @RequestParam(value = "employeeId", required = false) Long employeeId) {
        return ResponseEntity.ok(scheduleService.getSchedules(start, end, workGroupId, employeeId));
    }
}
