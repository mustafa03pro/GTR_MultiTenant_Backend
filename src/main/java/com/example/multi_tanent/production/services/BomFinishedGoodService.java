package com.example.multi_tanent.production.services;

import com.example.multi_tanent.production.dto.BomFinishedGoodRequest;
import com.example.multi_tanent.production.dto.BomFinishedGoodResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.ByteArrayInputStream;

public interface BomFinishedGoodService {
    BomFinishedGoodResponse create(BomFinishedGoodRequest request);

    BomFinishedGoodResponse update(Long id, BomFinishedGoodRequest request);

    BomFinishedGoodResponse getById(Long id);

    // Additional method to get by Item ID if needed?
    // Usually one item has one active BOM or multiple. Assuming ID access for now.

    Page<BomFinishedGoodResponse> getAll(Pageable pageable);

    void delete(Long id);

    ByteArrayInputStream exportToExcel(Long id);
}
