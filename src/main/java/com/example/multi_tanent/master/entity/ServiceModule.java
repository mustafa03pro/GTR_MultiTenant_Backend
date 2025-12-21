package com.example.multi_tanent.master.entity;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a single, selectable service module for a tenant.
 * Each module corresponds to a feature set and its associated database
 * entities.
 */
public enum ServiceModule {
    // Foundational module, always included.
    USER(true, "com.example.multi_tanent.spersusers.enitity"),

    // HRMS Modules
    HRMS_CORE(false, "com.example.multi_tanent.tenant.base.entity", "com.example.multi_tanent.tenant.employee.entity",
            USER),
    HRMS_ATTENDANCE(false, "com.example.multi_tanent.tenant.attendance.entity"),
    HRMS_LEAVE(false, "com.example.multi_tanent.tenant.leave.entity"),
    HRMS_PAYROLL(false, "com.example.multi_tanent.tenant.payroll.entity"),
    HRMS_RECRUITMENT(false, "com.example.multi_tanent.tenant.recruitment.entity"),

    // Point of Sale (POS) Module
    POS(false, "com.example.multi_tanent.pos.entity", USER),

    // CRM Module
    CRM(false, "com.example.multi_tanent.crm.entity", USER),

    // Production Module
    PRODUCTION(false, "com.example.multi_tanent.production.entity", USER),

    // Sales Module
    SALES(false, "com.example.multi_tanent.sales.entity", POS),

    // Purchase Module
    PURCHASES(false, "com.example.multi_tanent.purchases.entity", USER);

    private final boolean isFoundation;
    private final List<String> entityPackages;
    private final List<ServiceModule> dependencies;

    ServiceModule(boolean isFoundation, String entityPackage, ServiceModule... dependencies) {
        this.isFoundation = isFoundation;
        this.entityPackages = List.of(entityPackage);
        this.dependencies = List.of(dependencies);
    }

    ServiceModule(boolean isFoundation, String pkg1, String pkg2, ServiceModule... dependencies) {
        this.isFoundation = isFoundation;
        this.entityPackages = List.of(pkg1, pkg2);
        this.dependencies = List.of(dependencies);
    }

    public static String[] getPackagesForModules(List<ServiceModule> modules) {
        if (modules == null || modules.isEmpty()) {
            return new String[0];
        }

        // If the core HRMS module is present, we MUST also include the attendance
        // module
        // because TimeAttendence entity has a direct dependency on AttendancePolicy.
        if (modules.contains(HRMS_CORE)) {
            modules.add(HRMS_ATTENDANCE);
        }

        // Recursively collect all modules and their dependencies
        Set<ServiceModule> allModules = modules.stream()
                .flatMap(ServiceModule::getSelfAndAllDependencies)
                .collect(Collectors.toSet());

        // Always include foundational modules
        allModules.addAll(Arrays.stream(values())
                .filter(m -> m.isFoundation)
                .collect(Collectors.toSet()));

        return allModules.stream()
                .flatMap(module -> module.entityPackages.stream())
                .distinct()
                .toArray(String[]::new);
    }

    private Stream<ServiceModule> getSelfAndAllDependencies() {
        return Stream.concat(
                Stream.of(this),
                dependencies.stream().flatMap(ServiceModule::getSelfAndAllDependencies));
    }
}