// com/example/multi_tanent/tenant/repository/EmployeeRepository.java
package com.example.multi_tanent.tenant.employee.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.multi_tanent.spersusers.enitity.Employee;
import com.example.multi_tanent.spersusers.enums.EmployeeStatus;
import com.example.multi_tanent.spersusers.enums.Gender;
import com.example.multi_tanent.spersusers.enums.MartialStatus;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByUserId(Long userId);

    Optional<Employee> findByEmployeeCode(String employeeCode);

    Optional<Employee> findByIdAndUser_Tenant_Id(Long id, Long tenantId);

    List<Employee> findByGender(Gender gender);

    List<Employee> findByLocationId(Long locationId);

    List<Employee> findByMartialStatus(MartialStatus martialStatus);

    List<Employee> findByStatus(EmployeeStatus status);

    @Override
    List<Employee> findAll();

    boolean existsByEmployeeCode(String employeeCode);
}
