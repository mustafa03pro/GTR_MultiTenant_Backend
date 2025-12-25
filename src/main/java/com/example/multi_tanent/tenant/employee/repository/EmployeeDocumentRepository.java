package com.example.multi_tanent.tenant.employee.repository;

import com.example.multi_tanent.tenant.employee.entity.EmployeeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocument, Long> {
    List<EmployeeDocument> findByEmployeeId(Long employeeId);

    List<EmployeeDocument> findByEmployeeEmployeeCode(String employeeCode);

    @org.springframework.data.jpa.repository.Query("SELECT d FROM EmployeeDocument d JOIN FETCH d.employee e JOIN FETCH d.documentType dt WHERE e.user.tenant.tenantId = :tenantId")
    List<EmployeeDocument> findAllByTenantIdWithDetails(
            @org.springframework.data.repository.query.Param("tenantId") String tenantId);
}