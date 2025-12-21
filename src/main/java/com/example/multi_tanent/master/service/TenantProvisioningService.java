// com/example/multi_tanent/master/service/TenantProvisioningService.java
package com.example.multi_tanent.master.service;

import com.example.multi_tanent.config.TenantRegistry;
import com.example.multi_tanent.master.dto.ProvisionTenantRequest;
import com.example.multi_tanent.master.entity.SubscriptionStatus;
import com.example.multi_tanent.master.entity.ServiceModule;
import com.example.multi_tanent.master.entity.MasterTenant;
import com.example.multi_tanent.master.enums.Role;
import com.example.multi_tanent.spersusers.enitity.Store;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.spersusers.enitity.User;
import com.example.multi_tanent.master.repository.MasterTenantRepository; // Keep this line
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

import java.time.LocalDateTime;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class TenantProvisioningService {

    private final JdbcTemplate masterJdbc; // uses masterDataSource
    private final MasterTenantRepository masterRepo;
    private final TenantRegistry registry;
    private final PasswordEncoder passwordEncoder;

    // use ONE MySQL user that has CREATE DATABASE privilege for provisioning
    // e.g., same credentials that your tenants will use
    @Value("${provisioning.datasource.host}")
    private String mysqlHost;
    @Value("${provisioning.datasource.username}")
    private String mysqlUser;
    @Value("${provisioning.datasource.password}")
    private String mysqlPass;
    @Value("${provisioning.datasource.port}")
    private String mySqlPort;

    public TenantProvisioningService(
            DataSource masterDataSource,
            MasterTenantRepository masterRepo,
            TenantRegistry registry,
            PasswordEncoder passwordEncoder) {
        this.masterJdbc = new JdbcTemplate(masterDataSource);
        this.masterRepo = masterRepo;
        this.registry = registry;
        this.passwordEncoder = passwordEncoder;
    }

    // @Transactional - Removed to ensure masterTenant is saved even if schema
    // creation fails
    public void provision(ProvisionTenantRequest req) {
        // 1) Validate & normalize tenantId -> safe schema name like tenant_<id>
        String tenantId = normalizeTenantId(req.tenantId());
        String dbName = "tenant_" + tenantId; // final DB name

        // 2) CREATE DATABASE (needs MySQL user with CREATE privilege)
        // NOTE: backtick-quote the dbName & ensure it's safe beforehand
        masterJdbc.execute(
                "CREATE DATABASE IF NOT EXISTS `" + dbName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");

        // 3) Save to master_tenant
        String jdbcUrl = "jdbc:mysql://" + mysqlHost + ":" + mySqlPort + "/" + dbName
                + "?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true";
        MasterTenant mt = new MasterTenant();
        mt.setTenantId(tenantId);
        mt.setCompanyName(req.companyName());
        mt.setJdbcUrl(jdbcUrl);
        mt.setUsername(mysqlUser);
        mt.setPassword(mysqlPass);
        mt.setServiceModules(req.serviceModules());
        // Set new subscription fields
        mt.setNumberOfLocations(req.numberOfLocations());
        mt.setNumberOfUsers(req.numberOfUsers());
        mt.setNumberOfStore(req.numberOfStore());
        mt.setHrmsAccessCount(req.hrmsAccessCount());
        mt.setSubscriptionStartDate(req.subscriptionStartDate());
        mt.setSubscriptionEndDate(req.subscriptionEndDate());
        mt.setStatus(SubscriptionStatus.ACTIVE); // Or set based on your business logic
        masterRepo.save(mt);

        // 4) Create schema and seed initial admin users using a temporary data source
        try (HikariDataSource tenantDs = createTempDataSource(mt)) {
            createSchemaAndSeedData(tenantDs, mt, req);
        } catch (Exception e) {
            // If seeding fails, we should ideally roll back the DB creation,
            // but JDBC template doesn't participate in the transaction.
            // For now, we'll re-throw to ensure the master_tenant record is rolled back.
            throw new RuntimeException("Failed during tenant schema creation and seeding.", e);
        }

        // 5) Add/refresh DataSource in routing map for live application use.
        registry.addOrUpdateTenant(mt);
    }

    private String normalizeTenantId(String raw) {
        if (raw == null)
            throw new IllegalArgumentException("tenantId required");
        String id = raw.trim().toLowerCase();
        // only allow [a-z0-9_], replace others with _
        id = id.replaceAll("[^a-z0-9_]", "_");
        // basic guard
        if (!Pattern.matches("[a-z0-9_]{3,64}", id)) {
            throw new IllegalArgumentException("Invalid tenantId (use 3-64 of a-z,0-9,_)");
        }
        return id;
    }

    private HikariDataSource createTempDataSource(MasterTenant tenant) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(tenant.getJdbcUrl());
        ds.setUsername(tenant.getUsername());
        ds.setPassword(tenant.getPassword());
        // Optimization: We only need 1-2 connections for provisioning/schema updates
        ds.setMaximumPoolSize(2);
        ds.setMinimumIdle(0);
        return ds;
    }

    public void updateAdminRoles(MasterTenant tenant, Set<Role> newRoles) {
        if (newRoles == null || newRoles.isEmpty()) {
            // If no roles are provided, do nothing.
            return;
        }

        try (HikariDataSource tenantDs = createTempDataSource(tenant)) {
            LocalContainerEntityManagerFactoryBean emfBean = new LocalContainerEntityManagerFactoryBean();
            emfBean.setDataSource(tenantDs);
            // Scan all packages to ensure User entity is found
            emfBean.setPackagesToScan(ServiceModule.getPackagesForModules(tenant.getServiceModules()));
            emfBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            Properties p = new Properties();
            p.put("hibernate.hbm2ddl.auto", "none");
            p.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
            p.put("hibernate.show_sql", "true");
            emfBean.setJpaProperties(p);
            emfBean.afterPropertiesSet();

            EntityManagerFactory emf = emfBean.getObject();
            try (EntityManager em = emf.createEntityManager()) {
                em.getTransaction().begin();

                // Find the user with the TENANT_ADMIN role. This assumes there is one primary
                // admin.
                User adminUser = em.createQuery("SELECT u FROM User u JOIN u.roles r WHERE r IN (:roles)", User.class)
                        .setParameter("roles", Set.of(Role.HRMS_ADMIN, Role.POS_ADMIN))
                        .getResultStream().findFirst().orElse(null);

                if (adminUser != null) {
                    adminUser.setRoles(newRoles);
                    em.merge(adminUser);
                }
                em.getTransaction().commit();
            }
        }
    }

    private void createOrUpdateSchema(DataSource tenantDs, String ddlAction, String... packagesToScan) {
        LocalContainerEntityManagerFactoryBean emfBean = new LocalContainerEntityManagerFactoryBean();
        emfBean.setDataSource(tenantDs);
        emfBean.setPackagesToScan(packagesToScan);
        emfBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        Properties p = new Properties();
        // Use "create-drop" to avoid issues with dropping non-existent objects on a
        // fresh DB.
        p.put("hibernate.hbm2ddl.auto", "create");
        p.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        // p.put("hibernate.globally_quoted_identifiers", "true"); // Helps with
        // reserved keywords and case sensitivity
        emfBean.setJpaProperties(p);
        emfBean.afterPropertiesSet();
        emfBean.destroy(); // This triggers schema creation/update and then closes the factory.
    }

    private void createSchemaAndSeedData(DataSource tenantDs, MasterTenant masterTenant, ProvisionTenantRequest req) {
        // Step 1 & 2 Combined: Create the entire schema for all modules at once.
        createOrUpdateSchema(tenantDs, "create", ServiceModule.getPackagesForModules(req.serviceModules()));

        // Step 3: Now that schema is created, create a proper EMF to seed data.
        LocalContainerEntityManagerFactoryBean emfBean = new LocalContainerEntityManagerFactoryBean();
        emfBean.setDataSource(tenantDs);
        emfBean.setPackagesToScan(ServiceModule.getPackagesForModules(req.serviceModules()));
        emfBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        Properties p = new Properties();
        p.put("hibernate.hbm2ddl.auto", "none"); // Schema is already created, no need to modify it.
        p.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        emfBean.setJpaProperties(p);
        emfBean.afterPropertiesSet();

        EntityManagerFactory emf = emfBean.getObject();
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            // Create the tenant record within its own database first.
            Tenant tenantRecord = new Tenant();
            tenantRecord.setName(masterTenant.getCompanyName());
            tenantRecord.setTenantId(masterTenant.getTenantId()); // Explicitly set the tenantId
            em.persist(tenantRecord);

            // Seed a default Store for the tenant
            Store defaultStore = new Store();
            defaultStore.setTenant(tenantRecord);
            defaultStore.setName("Main Store");
            defaultStore.setAddress("Default Address");
            em.persist(defaultStore);

            // // Seed a default Tax Rate (e.g., VAT 5%)
            // TaxRate defaultTax = new TaxRate();
            // defaultTax.setTenant(tenantRecord);
            // defaultTax.setName("VAT 5%");
            // defaultTax.setPercent(new BigDecimal("5.00"));
            // em.persist(defaultTax);

            // // Seed a default Category
            // Category defaultCategory = new Category();
            // defaultCategory.setTenant(tenantRecord);
            // defaultCategory.setName("Default");
            // em.persist(defaultCategory);

            // Check if a user with this email already exists in the new tenant's DB.
            // This is a safeguard, though it should always be empty for a new tenant.
            long userCount = (long) em.createQuery("SELECT count(u) FROM User u WHERE u.email = :email")
                    .setParameter("email", req.adminEmail())
                    .getSingleResult();

            // Seed the initial admin user
            if (userCount == 0) {
                User admin = new User();
                admin.setTenant(tenantRecord);
                admin.setStore(defaultStore); // Associate admin with the default store
                admin.setName("Tenant Admin");
                admin.setEmail(req.adminEmail());
                admin.setPasswordHash(passwordEncoder.encode(req.adminPassword()));
                admin.setRoles(req.adminRoles()); // Use roles from the request
                admin.setIsActive(true);
                admin.setIsLocked(false);
                admin.setCreatedAt(LocalDateTime.now());
                admin.setUpdatedAt(LocalDateTime.now());
                admin.setLoginAttempts(0);
                em.persist(admin);
            }

            em.getTransaction().commit();
        } catch (Exception e) {
            // Re-throw to be handled by the main transactional method, which will cause a
            // rollback
            throw new RuntimeException("Failed to seed initial tenant data", e);
        } finally {
            emfBean.destroy(); // This also closes the EntityManagerFactory
        }
    }
}
