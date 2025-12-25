package com.example.multi_tanent.production.services;

import com.example.multi_tanent.config.TenantContext;
import com.example.multi_tanent.production.dto.ManufacturingOrderFileResponse;
import com.example.multi_tanent.production.dto.ManufacturingOrderRequest;
import com.example.multi_tanent.production.dto.ManufacturingOrderResponse;
import com.example.multi_tanent.production.entity.ManufacturingOrder;
import com.example.multi_tanent.production.entity.ManufacturingOrderFile;
import com.example.multi_tanent.production.enums.ManufacturingOrderStatus;
import com.example.multi_tanent.production.repository.BomSemiFinishedRepository;
import com.example.multi_tanent.production.repository.ManufacturingOrderFileRepository;
import com.example.multi_tanent.production.repository.ManufacturingOrderRepository;
import com.example.multi_tanent.production.repository.ProSemiFinishedRepository;
import com.example.multi_tanent.spersusers.enitity.Employee;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.spersusers.repository.BaseCustomerRepository;
import com.example.multi_tanent.tenant.employee.repository.EmployeeRepository;
import com.example.multi_tanent.spersusers.repository.LocationRepository;
import com.example.multi_tanent.spersusers.repository.TenantRepository;
import com.example.multi_tanent.tenant.service.FileStorageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import com.example.multi_tanent.production.repository.ManufacturingOrderSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional("tenantTx")
public class ManufacturingOrderService {

    private final ManufacturingOrderRepository OrderRepository;
    private final ManufacturingOrderFileRepository fileRepository;
    private final TenantRepository tenantRepository;
    private final LocationRepository locationRepository;
    private final ProSemiFinishedRepository itemRepository;
    private final BomSemiFinishedRepository bomRepository;
    private final BaseCustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final FileStorageService fileStorageService;
    private final com.example.multi_tanent.sales.repository.SalesOrderRepository salesOrderRepository;

    private Tenant getCurrentTenant() {
        String tenantId = TenantContext.getTenantId();
        return tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found: " + tenantId));
    }

    public List<ManufacturingOrderResponse> createFromSalesOrder(Long salesOrderId) {
        Tenant tenant = getCurrentTenant();
        com.example.multi_tanent.sales.entity.SalesOrder salesOrder = salesOrderRepository
                .findByIdAndTenantId(salesOrderId, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Sales Order not found with id: " + salesOrderId));

        List<ManufacturingOrder> createdOrders = new java.util.ArrayList<>();

        int index = 1;
        for (com.example.multi_tanent.sales.entity.SalesOrderItem soItem : salesOrder.getItems()) {
            // Find ProSemifinished item by itemCode
            var itemOpt = itemRepository.findByTenantIdAndItemCodeIgnoreCase(tenant.getId(), soItem.getItemCode());

            if (itemOpt.isPresent()) {
                var item = itemOpt.get();
                ManufacturingOrder mo = new ManufacturingOrder();
                mo.setTenant(tenant);
                String moNumber = "MO-" + salesOrder.getSalesOrderNumber() + "-" + index;

                if (!OrderRepository.existsByTenantIdAndMoNumber(tenant.getId(), moNumber)) {
                    mo.setMoNumber(moNumber);
                } else {
                    mo.setMoNumber(moNumber + "-" + System.currentTimeMillis());
                }

                mo.setSalesOrder(salesOrder);
                mo.setCustomer(salesOrder.getCustomer());
                mo.setReferenceNo(salesOrder.getReference());
                mo.setItem(item);
                mo.setQuantity(new java.math.BigDecimal(soItem.getQuantity()));
                mo.setStatus(ManufacturingOrderStatus.SCHEDULED);
                mo.setProductionHouse(item.getLocation()); // Default to item location

                createdOrders.add(OrderRepository.save(mo));
            }
            index++;
        }

        return createdOrders.stream()
                .map(ManufacturingOrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public ManufacturingOrderResponse create(ManufacturingOrderRequest request) {
        Tenant tenant = getCurrentTenant();
        if (OrderRepository.existsByTenantIdAndMoNumber(tenant.getId(), request.getMoNumber())) {
            throw new IllegalArgumentException(
                    "Manufacturing Order with MO Number '" + request.getMoNumber() + "' already exists.");
        }

        ManufacturingOrder entity = new ManufacturingOrder();
        mapRequestToEntity(request, entity, tenant);
        ManufacturingOrder savedEntity = OrderRepository.save(entity);

        ManufacturingOrderResponse response = ManufacturingOrderResponse.fromEntity(savedEntity);
        enrichFileUrls(response);
        return response;
    }

    @Transactional(value = "tenantTx", readOnly = true)
    public Page<ManufacturingOrderResponse> getAll(Pageable pageable, String search, ManufacturingOrderStatus status,
            LocalDate fromDate, LocalDate toDate) {
        Long tenantId = getCurrentTenant().getId();
        Specification<ManufacturingOrder> spec = ManufacturingOrderSpecification.getSpecifications(tenantId, search,
                status, fromDate, toDate);
        return OrderRepository.findAll(spec, pageable)
                .map(entity -> {
                    ManufacturingOrderResponse resp = ManufacturingOrderResponse.fromEntity(entity);
                    enrichFileUrls(resp);
                    return resp;
                });
    }

    @Transactional(value = "tenantTx", readOnly = true)
    public ManufacturingOrderResponse getById(Long id) {
        Long tenantId = getCurrentTenant().getId();
        ManufacturingOrder entity = OrderRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new EntityNotFoundException("Manufacturing Order not found with id: " + id));
        ManufacturingOrderResponse response = ManufacturingOrderResponse.fromEntity(entity);
        enrichFileUrls(response);
        return response;
    }

    public ManufacturingOrderResponse update(Long id, ManufacturingOrderRequest request) {
        Tenant tenant = getCurrentTenant();
        ManufacturingOrder entity = OrderRepository.findByTenantIdAndId(tenant.getId(), id)
                .orElseThrow(() -> new EntityNotFoundException("Manufacturing Order not found with id: " + id));

        if (!entity.getMoNumber().equalsIgnoreCase(request.getMoNumber()) &&
                OrderRepository.existsByTenantIdAndMoNumber(tenant.getId(), request.getMoNumber())) {
            throw new IllegalArgumentException(
                    "Manufacturing Order with MO Number '" + request.getMoNumber() + "' already exists.");
        }

        mapRequestToEntity(request, entity, tenant);
        ManufacturingOrder savedEntity = OrderRepository.save(entity);
        ManufacturingOrderResponse response = ManufacturingOrderResponse.fromEntity(savedEntity);
        enrichFileUrls(response);
        return response;
    }

    public void delete(Long id) {
        Tenant tenant = getCurrentTenant();
        ManufacturingOrder entity = OrderRepository.findByTenantIdAndId(tenant.getId(), id)
                .orElseThrow(() -> new EntityNotFoundException("Manufacturing Order not found with id: " + id));

        // Optionally delete files from storage
        // for (ManufacturingOrderFile file : entity.getFiles()) {
        // fileStorageService.deleteFile(file.getFilePath());
        // }

        OrderRepository.delete(entity);
    }

    public ManufacturingOrderFileResponse uploadFile(Long orderId, MultipartFile file) {
        Tenant tenant = getCurrentTenant();
        ManufacturingOrder order = OrderRepository.findByTenantIdAndId(tenant.getId(), orderId)
                .orElseThrow(() -> new EntityNotFoundException("Manufacturing Order not found with id: " + orderId));

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = "";
        if (originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // Request: upload/tenant_id/production/milisec.file_extension
        // FileStorageService.storeFile(bytes, subdirectory, filename) allows custom
        // filename.
        // We need to construct the filename "milisec.extension"
        String customFileName = System.currentTimeMillis() + extension;

        // Subdirectory under tenant folder: "production"
        String subDirectory = "production";

        try {
            // Using storeFile(byte[], subDirectory, filename) method I saw in
            // FileStorageService
            String relativePath = fileStorageService.storeFile(file.getBytes(), subDirectory, customFileName);

            ManufacturingOrderFile orderFile = ManufacturingOrderFile.builder()
                    .tenant(tenant)
                    .manufacturingOrder(order)
                    .fileName(originalFilename) // Store original name for display
                    .filePath(relativePath)
                    .build();

            ManufacturingOrderFile savedFile = fileRepository.save(orderFile);

            String fileUrl = fileStorageService.buildPublicUrl(savedFile.getFilePath());

            return ManufacturingOrderFileResponse.builder()
                    .id(savedFile.getId())
                    .fileName(savedFile.getFileName())
                    .fileUrl(fileUrl)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to store file for order " + orderId, e);
        }
    }

    public org.springframework.core.io.Resource getFileResource(Long fileId) {
        Tenant tenant = getCurrentTenant();
        ManufacturingOrderFile fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new EntityNotFoundException("File not found with id: " + fileId));

        if (!fileEntity.getTenant().getId().equals(tenant.getId())) {
            throw new SecurityException("Access denied to file with id: " + fileId);
        }

        return fileStorageService.loadFileAsResource(fileEntity.getFilePath(), true);
    }

    private void mapRequestToEntity(ManufacturingOrderRequest request, ManufacturingOrder entity, Tenant tenant) {
        entity.setTenant(tenant);
        entity.setMoNumber(request.getMoNumber());
        if (request.getSalesOrderId() != null) {
            entity.setSalesOrder(
                    salesOrderRepository.findByIdAndTenantId(request.getSalesOrderId(), tenant.getId()).orElse(null));
        } else {
            entity.setSalesOrder(null);
        }
        entity.setReferenceNo(request.getReferenceNo());
        entity.setQuantity(request.getQuantity());
        entity.setStatus(request.getStatus() != null ? request.getStatus() : ManufacturingOrderStatus.SCHEDULED);
        entity.setScheduleStart(request.getScheduleStart());
        entity.setScheduleFinish(request.getScheduleFinish());
        entity.setDueDate(request.getDueDate());
        entity.setActualStart(request.getActualStart());
        entity.setActualFinish(request.getActualFinish());
        entity.setBatchNo(request.getBatchNo());
        entity.setSamplingRequestStatus(request.getSamplingRequestStatus());

        if (request.getProductionHouseId() != null) {
            entity.setProductionHouse(locationRepository.findById(request.getProductionHouseId()).orElse(null));
        } else {
            entity.setProductionHouse(null);
        }

        if (request.getItemId() != null) {
            entity.setItem(itemRepository.findById(request.getItemId())
                    .orElseThrow(() -> new EntityNotFoundException("Item not found with id: " + request.getItemId())));
        }

        if (request.getBomId() != null) {
            entity.setBom(bomRepository.findById(request.getBomId()).orElse(null));
        } else {
            entity.setBom(null);
        }

        if (request.getCustomerId() != null) {
            entity.setCustomer(customerRepository.findById(request.getCustomerId()).orElse(null));
        } else {
            entity.setCustomer(null);
        }

        if (request.getAssignToId() != null) {
            entity.setAssignTo(employeeRepository.findById(request.getAssignToId()).orElse(null));

        } else {
            entity.setAssignTo(null);
        }
    }

    private void enrichFileUrls(ManufacturingOrderResponse response) {
        if (response.getFiles() != null) {
            response.getFiles().forEach(f -> {
                f.setFileUrl(fileStorageService.buildPublicUrl(f.getFileUrl())); // filePath mapped to fileUrl in DTO
                                                                                 // initially, here we expand it
            });
        }
    }

    public ByteArrayInputStream exportBom(Long orderId) {
        Tenant tenant = getCurrentTenant();
        ManufacturingOrder order = OrderRepository.findByTenantIdAndId(tenant.getId(), orderId)
                .orElseThrow(() -> new EntityNotFoundException("Manufacturing Order not found with id: " + orderId));

        if (order.getItem() == null) {
            throw new IllegalStateException("Manufacturing Order has no item associated.");
        }

        // Fetch BOM for the item
        // Assuming one active BOM or fetching the default one.
        // Order entity might track BOM used, but currently we look up via item or
        // order.getBom()
        // The Entity has `getBom()`.
        com.example.multi_tanent.production.entity.BomSemiFinished bom = order.getBom();
        if (bom == null) {
            // Fallback: try to find BOM by item if not set on order?
            // For now, if null, we can't export.
            throw new EntityNotFoundException("BOM not found for this order.");
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("BOM");

            // Header
            String[] columns = { "Sr.No", "Process Name", "Component Name", "Quantity Required", "Available Quantity",
                    "For Production Quantity Required", "UOM", "Rate/Unit", "Total Amount", "Notes" };
            Row headerRow = sheet.createRow(0);
            CellStyle headerCellStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerCellStyle.setFont(headerFont);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerCellStyle);
            }

            int rowIdx = 1;
            AtomicInteger srNo = new AtomicInteger(1);

            // Order quantity for calculation
            BigDecimal orderQty = order.getQuantity() != null ? order.getQuantity() : BigDecimal.ZERO;

            for (com.example.multi_tanent.production.entity.BomSemiFinishedDetail detail : bom.getDetails()) {
                Row row = sheet.createRow(rowIdx++);

                // Sr.No
                row.createCell(0).setCellValue(srNo.getAndIncrement());

                // Process Name
                String processName = detail.getProcess() != null ? detail.getProcess().getName() : "";
                row.createCell(1).setCellValue(processName);

                // Component Name & Details
                String componentName = "";
                String uom = "";
                BigDecimal rate = BigDecimal.ZERO;

                if (detail.getRawMaterial() != null) {
                    componentName = detail.getRawMaterial().getName();
                    uom = detail.getRawMaterial().getIssueUnit() != null
                            ? detail.getRawMaterial().getIssueUnit().getName()
                            : "";
                    rate = detail.getRawMaterial().getPurchasePrice(); // or costing price
                } else if (detail.getChildSemiFinished() != null) {
                    componentName = detail.getChildSemiFinished().getName();
                    uom = detail.getChildSemiFinished().getIssueUnit() != null
                            ? detail.getChildSemiFinished().getIssueUnit().getName()
                            : "";
                    rate = detail.getChildSemiFinished().getPurchasePrice(); // or sales/cost
                }

                row.createCell(2).setCellValue(componentName);

                // Quantity Required (Per Unit)
                BigDecimal qtyPerUnit = detail.getQuantity() != null ? detail.getQuantity() : BigDecimal.ZERO;
                row.createCell(3).setCellValue(qtyPerUnit.doubleValue());

                // Available Quantity (Placeholder 0 for now)
                row.createCell(4).setCellValue(0);

                // For Production Quantity Required (Total)
                BigDecimal totalQtyRequired = qtyPerUnit.multiply(orderQty);
                row.createCell(5).setCellValue(totalQtyRequired.doubleValue());

                // UOM
                row.createCell(6).setCellValue(uom);

                // Rate/Unit
                row.createCell(7).setCellValue(rate != null ? rate.doubleValue() : 0);

                // Total Amount
                BigDecimal totalAmount = (rate != null ? rate : BigDecimal.ZERO).multiply(totalQtyRequired);
                row.createCell(8).setCellValue(totalAmount.doubleValue());

                // Notes
                row.createCell(9).setCellValue(detail.getNotes() != null ? detail.getNotes() : "");
            }

            // Autosize columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to export BOM data to Excel file: " + e.getMessage());
        }
    }
}
