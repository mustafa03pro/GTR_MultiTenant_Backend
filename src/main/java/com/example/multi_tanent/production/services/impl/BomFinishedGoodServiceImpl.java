package com.example.multi_tanent.production.services.impl;

import com.example.multi_tanent.config.TenantContext;
import com.example.multi_tanent.production.dto.*;
import com.example.multi_tanent.production.entity.*;
import com.example.multi_tanent.production.repository.*;
import com.example.multi_tanent.production.services.BomFinishedGoodService;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.spersusers.repository.TenantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional("tenantTx")
public class BomFinishedGoodServiceImpl implements BomFinishedGoodService {

    private final BomFinishedGoodRepository repository;
    private final ProFinishedGoodRepository itemRepository;
    private final ProProcessRepository processRepository;
    private final ProRawMaterialsRepository rawMaterialRepository;
    private final ProSemiFinishedRepository semiFinishedRepository;
    private final ProcessFinishedGoodRepository routingRepository;
    private final ProUnitRepository unitRepository;
    private final TenantRepository tenantRepository;

    private Long getCurrentTenantId() {
        String tenantIdentifier = TenantContext.getTenantId();
        // Assuming numeric parsing or lookup logic similar to other services
        try {
            return Long.parseLong(tenantIdentifier);
        } catch (NumberFormatException e) {
            return tenantRepository.findByTenantId(tenantIdentifier)
                    .or(() -> tenantRepository.findByName(tenantIdentifier))
                    .orElseThrow(() -> new RuntimeException("Tenant not found for identifier: " + tenantIdentifier))
                    .getId();
        }
    }

    private Tenant getCurrentTenant() {
        Long id = getCurrentTenantId();
        Tenant t = new Tenant();
        t.setId(id);
        return t;
    }

    @Override
    public BomFinishedGoodResponse create(BomFinishedGoodRequest request) {
        Long tenantId = getCurrentTenantId();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);

        BomFinishedGood entity = new BomFinishedGood();
        entity.setTenant(tenant);

        mapRequestToEntity(request, entity, tenantId);

        BomFinishedGood saved = repository.save(entity);
        return toResponse(saved);
    }

    @Override
    public BomFinishedGoodResponse update(Long id, BomFinishedGoodRequest request) {
        Long tenantId = getCurrentTenantId();
        BomFinishedGood entity = repository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new EntityNotFoundException("BOM not found"));

        mapRequestToEntity(request, entity, tenantId);
        BomFinishedGood saved = repository.save(entity);
        return toResponse(saved);
    }

    @Override
    public BomFinishedGoodResponse getById(Long id) {
        Long tenantId = getCurrentTenantId();
        BomFinishedGood entity = repository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new EntityNotFoundException("BOM not found"));
        return toResponse(entity);
    }

    @Override
    public Page<BomFinishedGoodResponse> getAll(Pageable pageable) {
        Long tenantId = getCurrentTenantId();
        return repository.findByTenantId(tenantId, pageable).map(this::toResponse);
    }

    @Override
    public void delete(Long id) {
        Long tenantId = getCurrentTenantId();
        BomFinishedGood entity = repository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new EntityNotFoundException("BOM not found"));
        repository.delete(entity);
    }

    @Override
    public ByteArrayInputStream exportToExcel(Long id) {
        BomFinishedGoodResponse bom = getById(id);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("BOM Information");

            // Header Styles
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            // General Info
            int rowIdx = 0;
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue("Item");
            row.createCell(1).setCellValue(bom.getItem().getName());

            row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue("Quantity");
            row.createCell(1).setCellValue(bom.getQuantity().doubleValue());

            row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue("BOM Name");
            row.createCell(1).setCellValue(bom.getBomName());

            row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue("Routing");
            row.createCell(1).setCellValue(bom.getRouting() != null ? bom.getRouting().getProcessFlowName() : "N/A");

            row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue("Approx Cost");
            row.createCell(1)
                    .setCellValue(bom.getApproximateCost() != null ? bom.getApproximateCost().doubleValue() : 0.0);

            // Spacer
            rowIdx++;

            // Components Table Header
            Row tableHeader = sheet.createRow(rowIdx++);
            String[] headers = { "Sr.No", "Process Name", "Component Name", "Quantity Required", "UOM", "Rate/Unit",
                    "Total Amount", "Notes" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = tableHeader.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Components Data
            for (BomFinishedGoodDetailResponse detail : bom.getDetails()) {
                Row dataRow = sheet.createRow(rowIdx++);
                dataRow.createCell(0).setCellValue(detail.getSequence());
                dataRow.createCell(1).setCellValue(detail.getProcess() != null ? detail.getProcess().getName() : "");

                String componentName = "";
                if (detail.getRawMaterial() != null)
                    componentName = detail.getRawMaterial().getName();
                else if (detail.getSemiFinished() != null)
                    componentName = detail.getSemiFinished().getName();
                dataRow.createCell(2).setCellValue(componentName);

                dataRow.createCell(3).setCellValue(detail.getQuantity().doubleValue());
                dataRow.createCell(4).setCellValue(detail.getUom() != null ? detail.getUom().getName() : "");
                dataRow.createCell(5).setCellValue(detail.getRate() != null ? detail.getRate().doubleValue() : 0.0);
                dataRow.createCell(6).setCellValue(detail.getAmount() != null ? detail.getAmount().doubleValue() : 0.0);
                dataRow.createCell(7).setCellValue(detail.getNotes());
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to export Excel", e);
        }
    }

    private void mapRequestToEntity(BomFinishedGoodRequest request, BomFinishedGood entity, Long tenantId) {
        entity.setBomName(request.getBomName());
        entity.setQuantity(request.getQuantity());
        entity.setApproximateCost(request.getApproximateCost());
        entity.setActive(request.isActive());

        // Fetch and set relations
        ProFinishedGood item = itemRepository.findByTenantIdAndId(tenantId, request.getItemId())
                .orElseThrow(() -> new EntityNotFoundException("Item not found"));
        entity.setItem(item);
        entity.setLocation(item.getLocation()); // Inherit location

        if (request.getRoutingId() != null) {
            ProcessFinishedGood routing = routingRepository.findByTenantIdAndId(tenantId, request.getRoutingId())
                    .orElseThrow(() -> new EntityNotFoundException("Routing process not found"));
            entity.setRouting(routing);
        }

        // Details
        if (entity.getDetails() == null) {
            entity.setDetails(new ArrayList<>());
        }
        entity.getDetails().clear();

        if (request.getDetails() != null) {
            Tenant tenant = new Tenant();
            tenant.setId(tenantId);

            for (BomFinishedGoodDetailRequest req : request.getDetails()) {
                BomFinishedGoodDetail detail = new BomFinishedGoodDetail();
                detail.setTenant(tenant);
                detail.setLocation(entity.getLocation());
                detail.setBomFinishedGood(entity);
                detail.setQuantity(req.getQuantity());
                detail.setRate(req.getRate());
                detail.setAmount(req.getAmount());
                detail.setNotes(req.getNotes());
                detail.setSequence(req.getSequence());

                if (req.getProcessId() != null) {
                    ProProcess process = processRepository.findByTenantIdAndId(tenantId, req.getProcessId())
                            .orElse(null);
                    detail.setProcess(process);
                }

                if (req.getRawMaterialId() != null) {
                    ProRawMaterials rm = rawMaterialRepository.findByTenantIdAndId(tenantId, req.getRawMaterialId())
                            .orElse(null);
                    detail.setRawMaterial(rm);
                } else if (req.getSemiFinishedId() != null) {
                    ProSemifinished sf = semiFinishedRepository.findByTenantIdAndId(tenantId, req.getSemiFinishedId())
                            .orElse(null);
                    detail.setSemiFinished(sf);
                }

                if (req.getUomId() != null) {
                    ProUnit unit = unitRepository.findByTenantIdAndId(tenantId, req.getUomId())
                            .orElse(null);
                    detail.setUom(unit);
                }

                entity.getDetails().add(detail);
            }
        }
    }

    private BomFinishedGoodResponse toResponse(BomFinishedGood entity) {
        List<BomFinishedGoodDetailResponse> detailResponses = entity.getDetails().stream()
                .map(detail -> BomFinishedGoodDetailResponse.builder()
                        .id(detail.getId())
                        .process(
                                detail.getProcess() != null ? ProProcessResponse.fromEntity(detail.getProcess()) : null)
                        .rawMaterial(detail.getRawMaterial() != null
                                ? ProRawMaterialsResponse.fromEntity(detail.getRawMaterial())
                                : null)
                        .semiFinished(detail.getSemiFinished() != null
                                ? ProSemiFinishedResponse.fromEntity(detail.getSemiFinished())
                                : null)
                        .quantity(detail.getQuantity())
                        .uom(detail.getUom() != null ? ProUnitResponse.fromEntity(detail.getUom()) : null)
                        .rate(detail.getRate())
                        .amount(detail.getAmount())
                        .notes(detail.getNotes())
                        .sequence(detail.getSequence())
                        .build())
                .collect(Collectors.toList());

        return BomFinishedGoodResponse.builder()
                .id(entity.getId())
                .item(ProFinishedGoodResponse.fromEntity(entity.getItem()))
                .bomName(entity.getBomName())
                .quantity(entity.getQuantity())
                .routing(entity.getRouting() != null ? ProcessFinishedGoodResponse.fromEntity(entity.getRouting())
                        : null)
                .approximateCost(entity.getApproximateCost())
                .isActive(entity.isActive())
                .details(detailResponses)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
