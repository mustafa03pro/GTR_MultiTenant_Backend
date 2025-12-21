package com.example.multi_tanent.config;

import com.example.multi_tanent.master.entity.MasterTenant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Properties;

@Component
public class TenantSchemaCreator {

    private void createOrUpdateSchema(DataSource ds, String... packagesToScan) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(ds);
        emf.setPackagesToScan(packagesToScan);
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        Properties props = new Properties();
        props.put("hibernate.hbm2ddl.auto", "update");
        props.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        emf.setJpaProperties(props);
        emf.afterPropertiesSet();
        emf.destroy(); // This triggers the schema update and closes the factory.
    }

    private void migrateData(DataSource ds) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);
        try {
            // First, ensure a tenant record exists.
            int tenantCount = jdbcTemplate.queryForObject("SELECT count(*) FROM tenants", Integer.class);
            if (tenantCount == 0) {
                // This part is tricky as we don't have the company name here directly.
                // For migration, we can use a placeholder or fetch it if possible.
                // Let's assume a placeholder is acceptable for the migration script.
                System.out.println("INFO: No tenant record found. Seeding one for migration.");
                jdbcTemplate.update("INSERT INTO tenants (name, created_at) VALUES ('Migrated Tenant', NOW())");
            }

            // Find the ID of the single tenant record in this database.
            Long tenantRecordId = jdbcTemplate.queryForObject("SELECT id FROM tenants LIMIT 1", Long.class);

            if (tenantRecordId != null && tenantRecordId > 0) {
                // Update all users that don't have a tenant_id set yet.
                String sql = "UPDATE users SET tenant_id = ? WHERE tenant_id IS NULL";
                int rowsAffected = jdbcTemplate.update(sql, tenantRecordId);
                if (rowsAffected > 0) {
                    System.out.println("INFO: Migrated " + rowsAffected + " users to tenant_id: " + tenantRecordId);
                }
            }

            // FIX: Manually relax the quotation_id constraint for FollowUps to allow nulls
            try {
                jdbcTemplate.execute("ALTER TABLE rental_quotation_followups MODIFY quotation_id BIGINT NULL");
                System.out.println("INFO: Relaxed quotation_id constraint on rental_quotation_followups");
            } catch (Exception e) {
                // Ignore if table doesn't exist or other error, it might be fine on fresh
                // install
                System.out
                        .println("INFO: Could not relax quotation_id constraint (might be fresh install or unrelated): "
                                + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println(
                    "WARN: Data migration for tenant_id failed. This might be expected if the tables don't exist yet. Error: "
                            + e.getMessage());
        }
    }

    public void ensureSchema(DataSource ds, MasterTenant tenant) {
        if (tenant == null || tenant.getServiceModules() == null || tenant.getServiceModules().isEmpty()) {
            System.err.println("WARNING: No modules specified for tenant '"
                    + (tenant != null ? tenant.getTenantId() : "null") + "'. Skipping schema creation.");
            return;
        }
        // Step 1: Run update to add new columns as nullable
        createOrUpdateSchema(ds, tenant.getEntityPackages());
        // Step 2: Run data migration to populate the new non-nullable columns
        migrateData(ds);
        // Step 3: Run update again to apply NOT NULL constraints
        createOrUpdateSchema(ds, tenant.getEntityPackages());

        // Step 4: Fix Foreign Key constraints
        fixDeliveryOrderForeignKey(ds, tenant.getTenantId());
    }

    private void fixDeliveryOrderForeignKey(DataSource ds, String tenantId) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);
        try {
            System.out.println("Checking schema for tenant: " + tenantId);

            // 1. Identify if the wrong constraint exists
            String findWrongConstraintSql = "SELECT CONSTRAINT_NAME " +
                    "FROM information_schema.KEY_COLUMN_USAGE " +
                    "WHERE TABLE_NAME = 'delivery_orders' " +
                    "AND REFERENCED_TABLE_NAME = 'sales_customers' " +
                    "AND TABLE_SCHEMA = DATABASE()";

            java.util.List<String> wrongConstraints = jdbcTemplate.queryForList(findWrongConstraintSql, String.class);

            if (!wrongConstraints.isEmpty()) {
                for (String constraintName : wrongConstraints) {
                    System.out.println("[" + tenantId + "] Found invalid Foreign Key constraint: " + constraintName
                            + " referencing sales_customers. Dropping it...");
                    String dropSql = "ALTER TABLE delivery_orders DROP FOREIGN KEY " + constraintName;
                    jdbcTemplate.execute(dropSql);
                    System.out.println("[" + tenantId + "] Successfully dropped constraint: " + constraintName);
                }

                // 2. Add the correct constraint
                String findCorrectConstraintSql = "SELECT CONSTRAINT_NAME " +
                        "FROM information_schema.KEY_COLUMN_USAGE " +
                        "WHERE TABLE_NAME = 'delivery_orders' " +
                        "AND REFERENCED_TABLE_NAME = 'base_customers' " +
                        "AND TABLE_SCHEMA = DATABASE()";

                java.util.List<String> correctConstraints = jdbcTemplate.queryForList(findCorrectConstraintSql,
                        String.class);

                if (correctConstraints.isEmpty()) {
                    System.out.println("[" + tenantId + "] Adding correct Foreign Key constraint to base_customers...");
                    String addSql = "ALTER TABLE delivery_orders " +
                            "ADD CONSTRAINT fk_delivery_orders_base_customers " +
                            "FOREIGN KEY (customer_id) REFERENCES base_customers (id)";
                    jdbcTemplate.execute(addSql);
                    System.out.println(
                            "[" + tenantId + "] Successfully added constraint: fk_delivery_orders_base_customers");
                }
            } else {
                String findCorrectConstraintSql = "SELECT CONSTRAINT_NAME " +
                        "FROM information_schema.KEY_COLUMN_USAGE " +
                        "WHERE TABLE_NAME = 'delivery_orders' " +
                        "AND REFERENCED_TABLE_NAME = 'base_customers' " +
                        "AND TABLE_SCHEMA = DATABASE()";

                java.util.List<String> correctConstraints = jdbcTemplate.queryForList(findCorrectConstraintSql,
                        String.class);
                if (correctConstraints.isEmpty()) {
                    System.out.println(
                            "[" + tenantId + "] Adding correct Foreign Key constraint to base_customers (missing)...");
                    try {
                        String addSql = "ALTER TABLE delivery_orders " +
                                "ADD CONSTRAINT fk_delivery_orders_base_customers " +
                                "FOREIGN KEY (customer_id) REFERENCES base_customers (id)";
                        jdbcTemplate.execute(addSql);
                        System.out.println(
                                "[" + tenantId + "] Successfully added constraint: fk_delivery_orders_base_customers");
                    } catch (Exception e) {
                        System.err.println("[" + tenantId
                                + "] Could not add base_customers constraint. Maybe data inconsistency? Error: "
                                + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            System.err
                    .println("[" + tenantId + "] Failed to execute schema fix for delivery_orders: " + e.getMessage());
        }
    }
}
