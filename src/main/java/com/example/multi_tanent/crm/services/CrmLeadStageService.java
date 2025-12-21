package com.example.multi_tanent.crm.services;

import com.example.multi_tanent.config.TenantContext;
import com.example.multi_tanent.crm.dto.CrmLeadStageRequest;
import com.example.multi_tanent.crm.dto.CrmLeadStageResponse;
import com.example.multi_tanent.crm.entity.CrmLeadStage;
import com.example.multi_tanent.crm.repository.CrmLeadStageRepository;
import com.example.multi_tanent.spersusers.enitity.Location;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.spersusers.repository.LocationRepository;
import com.example.multi_tanent.spersusers.repository.TenantRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional("tenantTx")
public class CrmLeadStageService {

    private final CrmLeadStageRepository repo;
    private final TenantRepository tenantRepo;
    private final LocationRepository locationRepository;

    private Tenant currentTenant() {
        String tenantId = TenantContext.getTenantId();
        return tenantRepo.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("Tenant not found for tenantId: " + tenantId));
    }

    @Transactional(readOnly = true)
    public List<CrmLeadStageResponse> getAll() {
        Tenant t = currentTenant();
        return repo.findByTenantIdOrderBySortOrderAscIdAsc(t.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CrmLeadStageResponse getById(Long id) {
        Tenant t = currentTenant();
        CrmLeadStage e = repo.findByIdAndTenantId(id, t.getId())
                .orElseThrow(() -> new EntityNotFoundException("Lead stage not found: " + id));
        return toResponse(e);
    }

    public CrmLeadStageResponse create(CrmLeadStageRequest req) {
        Tenant t = currentTenant();
        String name = req.getName().trim();

        if (repo.existsByTenantIdAndNameIgnoreCase(t.getId(), name)) {
            throw new IllegalArgumentException("Lead stage already exists: " + name);
        }

        int nextOrder = repo.findFirstByTenantIdOrderBySortOrderDesc(t.getId())
                .map(s -> s.getSortOrder() + 1).orElse(1);

        Location location = null;
        if (req.getLocationId() != null) {
            location = locationRepository.findById(req.getLocationId())
                    .orElseThrow(
                            () -> new EntityNotFoundException("Location not found with id: " + req.getLocationId()));
        }

        CrmLeadStage e = CrmLeadStage.builder()
                .tenant(t)
                .location(location)
                .name(name)
                .isDefault(req.isDefault())
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : nextOrder)
                .moveTo(req.getMoveTo())
                .build();

        if (e.isDefault()) {
            repo.findFirstByTenantIdAndIsDefaultTrue(t.getId())
                    .ifPresent(prev -> {
                        prev.setDefault(false);
                        repo.save(prev);
                    });
        }

        return toResponse(repo.save(e));
    }

    public CrmLeadStageResponse update(Long id, CrmLeadStageRequest req) {
        Tenant t = currentTenant();
        CrmLeadStage e = repo.findByIdAndTenantId(id, t.getId())
                .orElseThrow(() -> new EntityNotFoundException("Lead stage not found: " + id));

        if (req.getName() != null) {
            String newName = req.getName().trim();
            if (!e.getName().equalsIgnoreCase(newName)
                    && repo.existsByTenantIdAndNameIgnoreCase(t.getId(), newName)) {
                throw new IllegalArgumentException("Stage name already exists: " + newName);
            }
            e.setName(newName);
        }

        if (req.getLocationId() != null) {
            Location location = locationRepository.findById(req.getLocationId())
                    .orElseThrow(
                            () -> new EntityNotFoundException("Location not found with id: " + req.getLocationId()));
            e.setLocation(location);
        } else {
            e.setLocation(null);
        }

        if (req.getMoveTo() != null) {
            e.setMoveTo(req.getMoveTo());
        }

        if (req.getSortOrder() != null)
            e.setSortOrder(req.getSortOrder());

        if (req.isDefault() != e.isDefault()) {
            if (req.isDefault()) {
                repo.findFirstByTenantIdAndIsDefaultTrue(t.getId())
                        .ifPresent(prev -> {
                            prev.setDefault(false);
                            repo.save(prev);
                        });
                e.setDefault(true);
            } else {
                e.setDefault(false);
            }
        }

        return toResponse(repo.save(e));
    }

    public void delete(Long id) {
        CrmLeadStage e = repo.findByIdAndTenantId(id, currentTenant().getId())
                .orElseThrow(() -> new EntityNotFoundException("Lead stage not found: " + id));
        repo.delete(e);
    }

    /** Move up (swap with previous by sortOrder). */
    public CrmLeadStageResponse moveUp(Long id) {
        Tenant t = currentTenant();
        CrmLeadStage e = repo.findByIdAndTenantId(id, t.getId())
                .orElseThrow(() -> new EntityNotFoundException("Lead stage not found: " + id));

        var prev = repo.findFirstByTenantIdAndSortOrderLessThanOrderBySortOrderDesc(t.getId(), e.getSortOrder())
                .orElse(null);
        if (prev == null)
            return toResponse(e);

        int tmp = e.getSortOrder();
        e.setSortOrder(prev.getSortOrder());
        prev.setSortOrder(tmp);
        repo.save(prev);
        return toResponse(repo.save(e));
    }

    /** Move down (swap with next by sortOrder). */
    public CrmLeadStageResponse moveDown(Long id) {
        Tenant t = currentTenant();
        CrmLeadStage e = repo.findByIdAndTenantId(id, t.getId())
                .orElseThrow(() -> new EntityNotFoundException("Lead stage not found: " + id));

        var next = repo.findFirstByTenantIdAndSortOrderGreaterThanOrderBySortOrderAsc(t.getId(), e.getSortOrder())
                .orElse(null);
        if (next == null)
            return toResponse(e);

        int tmp = e.getSortOrder();
        e.setSortOrder(next.getSortOrder());
        next.setSortOrder(tmp);
        repo.save(next);
        return toResponse(repo.save(e));
    }

    /** Mark one as default (unset previous). */
    public CrmLeadStageResponse setDefault(Long id) {
        Tenant t = currentTenant();
        CrmLeadStage e = repo.findByIdAndTenantId(id, t.getId())
                .orElseThrow(() -> new EntityNotFoundException("Lead stage not found: " + id));

        repo.findFirstByTenantIdAndIsDefaultTrue(t.getId())
                .ifPresent(prev -> {
                    if (!prev.getId().equals(e.getId())) {
                        prev.setDefault(false);
                        repo.save(prev);
                    }
                });
        e.setDefault(true);
        return toResponse(repo.save(e));
    }

    private CrmLeadStageResponse toResponse(CrmLeadStage e) {
        CrmLeadStageResponse.CrmLeadStageResponseBuilder builder = CrmLeadStageResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .isDefault(e.isDefault())
                .sortOrder(e.getSortOrder())
                .moveTo(e.getMoveTo())
                .tenantId(e.getTenant() != null ? e.getTenant().getId() : null);

        if (e.getLocation() != null) {
            builder.locationId(e.getLocation().getId());
            builder.locationName(e.getLocation().getName());
        }

        return builder.build();
    }
}