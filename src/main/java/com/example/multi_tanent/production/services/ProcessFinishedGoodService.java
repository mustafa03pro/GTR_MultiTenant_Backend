package com.example.multi_tanent.production.services;

import com.example.multi_tanent.config.TenantContext;
import com.example.multi_tanent.production.dto.ProcessFinishedGoodDetailRequest;
import com.example.multi_tanent.production.dto.ProcessFinishedGoodDetailResponse;
import com.example.multi_tanent.production.dto.ProcessFinishedGoodRequest;
import com.example.multi_tanent.production.dto.ProcessFinishedGoodResponse;
import com.example.multi_tanent.production.entity.*;
import com.example.multi_tanent.production.repository.*;
import com.example.multi_tanent.spersusers.enitity.Location;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.spersusers.repository.LocationRepository;
import com.example.multi_tanent.spersusers.repository.TenantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional("tenantTx")
public class ProcessFinishedGoodService {

    private final ProcessFinishedGoodRepository repository;
    private final ProFinishedGoodRepository itemRepository;
    private final ProProcessRepository processRepository;
    private final ProWorkGroupRepository workGroupRepository;
    private final TenantRepository tenantRepository;
    private final LocationRepository locationRepository;

    private Tenant getCurrentTenant() {
        String tenantId = TenantContext.getTenantId();
        return tenantRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("Tenant not found for tenantId: " + tenantId));
    }

    public ProcessFinishedGoodResponse create(ProcessFinishedGoodRequest request) {
        Tenant tenant = getCurrentTenant();
        ProcessFinishedGood entity = new ProcessFinishedGood();
        mapRequestToEntity(request, entity, tenant);
        ProcessFinishedGood savedEntity = repository.save(entity);
        return toResponse(savedEntity);
    }

    public ProcessFinishedGoodResponse update(Long id, ProcessFinishedGoodRequest request) {
        ProcessFinishedGood entity = repository.findByIdAndTenantId(id, getCurrentTenant().getId())
                .orElseThrow(() -> new EntityNotFoundException("ProcessFinishedGood not found with id: " + id));
        mapRequestToEntity(request, entity, entity.getTenant());
        ProcessFinishedGood updatedEntity = repository.save(entity);
        return toResponse(updatedEntity);
    }

    public ProcessFinishedGoodResponse getById(Long id) {
        return repository.findByIdAndTenantId(id, getCurrentTenant().getId())
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("ProcessFinishedGood not found with id: " + id));
    }

    public Page<ProcessFinishedGoodResponse> getAll(Pageable pageable) {
        return repository.findByTenantId(getCurrentTenant().getId(), pageable)
                .map(this::toResponse);
    }

    public void delete(Long id) {
        ProcessFinishedGood entity = repository.findByIdAndTenantId(id, getCurrentTenant().getId())
                .orElseThrow(() -> new EntityNotFoundException("ProcessFinishedGood not found with id: " + id));
        repository.delete(entity);
    }

    private void mapRequestToEntity(ProcessFinishedGoodRequest request, ProcessFinishedGood entity, Tenant tenant) {
        entity.setTenant(tenant);
        entity.setProcessFlowName(request.getProcessFlowName());
        entity.setOtherFixedCost(request.getOtherFixedCost());
        entity.setOtherVariableCost(request.getOtherVariableCost());
        entity.setLocked(request.isLocked());

        if (request.getLocationId() != null) {
            Location location = locationRepository.findById(request.getLocationId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Location not found with id: " + request.getLocationId()));
            entity.setLocation(location);
        } else {
            entity.setLocation(null);
        }

        ProFinishedGood item = itemRepository.findByTenantIdAndId(tenant.getId(), request.getItemId())
                .orElseThrow(() -> new EntityNotFoundException("Item not found with id: " + request.getItemId()));
        entity.setFinishedGood(item);

        // Handle details
        if (entity.getDetails() == null) {
            entity.setDetails(new ArrayList<>());
        }
        entity.getDetails().clear();

        if (request.getDetails() != null) {
            List<ProcessFinishedGoodDetail> details = request.getDetails().stream().map(detailReq -> {
                ProcessFinishedGoodDetail detail = new ProcessFinishedGoodDetail();
                detail.setTenant(tenant);
                detail.setLocation(entity.getLocation());
                detail.setProcessFinishedGood(entity);
                detail.setSetupTime(detailReq.getSetupTime());
                detail.setCycleTime(detailReq.getCycleTime());
                detail.setFixedCost(detailReq.getFixedCost());
                detail.setVariableCost(detailReq.getVariableCost());
                detail.setOutsource(detailReq.isOutsource());
                detail.setTesting(detailReq.isTesting());
                detail.setNotes(detailReq.getNotes());
                detail.setSequence(detailReq.getSequence());

                if (detailReq.getProcessId() != null) {
                    ProProcess process = processRepository.findById(detailReq.getProcessId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Process not found with id: " + detailReq.getProcessId()));
                    detail.setProcess(process);
                }

                if (detailReq.getWorkGroupId() != null) {
                    ProWorkGroup workGroup = workGroupRepository.findById(detailReq.getWorkGroupId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "WorkGroup not found with id: " + detailReq.getWorkGroupId()));
                    detail.setWorkGroup(workGroup);
                }

                return detail;
            }).collect(Collectors.toList());
            entity.getDetails().addAll(details);
        }
    }

    private ProcessFinishedGoodResponse toResponse(ProcessFinishedGood entity) {
        List<ProcessFinishedGoodDetailResponse> detailResponses = entity.getDetails().stream()
                .map(detail -> ProcessFinishedGoodDetailResponse.builder()
                        .id(detail.getId())
                        .processId(detail.getProcess() != null ? detail.getProcess().getId() : null)
                        .processName(detail.getProcess() != null ? detail.getProcess().getName() : null)
                        .workGroupId(detail.getWorkGroup() != null ? detail.getWorkGroup().getId() : null)
                        .workGroupName(detail.getWorkGroup() != null ? detail.getWorkGroup().getName() : null)
                        .setupTime(detail.getSetupTime())
                        .cycleTime(detail.getCycleTime())
                        .fixedCost(detail.getFixedCost())
                        .variableCost(detail.getVariableCost())
                        .isOutsource(detail.isOutsource())
                        .isTesting(detail.isTesting())
                        .notes(detail.getNotes())
                        .sequence(detail.getSequence())
                        .build())
                .collect(Collectors.toList());

        return ProcessFinishedGoodResponse.builder()
                .id(entity.getId())
                .itemId(entity.getFinishedGood().getId())
                .itemName(entity.getFinishedGood().getName())
                .processFlowName(entity.getProcessFlowName())
                .otherFixedCost(entity.getOtherFixedCost())
                .otherVariableCost(entity.getOtherVariableCost())
                .isLocked(entity.isLocked())
                .details(detailResponses)
                .locationId(entity.getLocation() != null ? entity.getLocation().getId() : null)
                .locationName(entity.getLocation() != null ? entity.getLocation().getName() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
