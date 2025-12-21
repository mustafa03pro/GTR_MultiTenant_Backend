package com.example.multi_tanent.tenant.payroll.controller;

import com.example.multi_tanent.spersusers.enitity.CompanyLocation;
import com.example.multi_tanent.tenant.payroll.dto.CompanyLocationRequest;
import com.example.multi_tanent.tenant.payroll.dto.CompanyLocationResponse;
import com.example.multi_tanent.tenant.payroll.service.CompanyLocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/company-locations")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','HRMS_ADMIN','HR','MANAGER')")
public class CompanyLocationController {

    private final CompanyLocationService locationService;

    public CompanyLocationController(CompanyLocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    public ResponseEntity<List<CompanyLocationResponse>> getAllLocations() {
        List<CompanyLocationResponse> locations = locationService.getAllLocations().stream()
                .map(CompanyLocationResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(locations);
    }

    @PostMapping
    public ResponseEntity<CompanyLocationResponse> createLocation(@RequestBody CompanyLocationRequest request) {
        CompanyLocation createdLocation = locationService.createLocation(request);
        URI locationUri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(createdLocation.getId()).toUri();
        return ResponseEntity.created(locationUri).body(CompanyLocationResponse.fromEntity(createdLocation));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompanyLocationResponse> updateLocation(@PathVariable Long id,
            @RequestBody CompanyLocationRequest request) {
        return ResponseEntity.ok(CompanyLocationResponse.fromEntity(locationService.updateLocation(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocation(@PathVariable Long id) {
        locationService.deleteLocation(id);
        return ResponseEntity.noContent().build();
    }
}
