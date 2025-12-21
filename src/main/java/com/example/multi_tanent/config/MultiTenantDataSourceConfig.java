// com/example/multi_tanent/config/MultiTenantDataSourceConfig.java
package com.example.multi_tanent.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Properties;

@Configuration
@EnableJpaRepositories(basePackages = {
    "com.example.multi_tanent.tenant",
    "com.example.multi_tanent.pos.repository",
    "com.example.multi_tanent.spersusers.repository",
    "com.example.multi_tanent.crm.repository",
    "com.example.multi_tanent.production.repository",
    "com.example.multi_tanent.sales.repository",
    "com.example.multi_tanent.purchases.repository"
}, entityManagerFactoryRef = "tenantEmf", transactionManagerRef = "tenantTx")
public class MultiTenantDataSourceConfig {

  @Bean(name = "tenantRoutingDataSource")
  public TenantRoutingDataSource tenantRoutingDataSource(
      @Qualifier("masterDataSource") DataSource master) {
    TenantRoutingDataSource routing = new TenantRoutingDataSource();
    // set an EMPTY map so AbstractRoutingDataSource is happy
    routing.setTargetDataSources(new HashMap<>()); // 👈 not null
    routing.setDefaultTargetDataSource(master);
    routing.afterPropertiesSet();
    return routing;
  }

  // @Bean(name="tenantDataSource")
  // public DataSource tenantDataSource(@Qualifier("tenantRoutingDataSource")
  // TenantRoutingDataSource routing) {
  // return routing; // not @Primary
  // }

  @Bean(name = "tenantEmf")
  @Primary
  public LocalContainerEntityManagerFactoryBean tenantEmf(
      @Qualifier("tenantRoutingDataSource") DataSource ds) {
    var emf = new LocalContainerEntityManagerFactoryBean();
    emf.setDataSource(ds);
    // Scan all possible tenant entity packages so the EntityManager is aware of
    // them.
    // The TenantSchemaCreator will still only create tables for the tenant's
    // specific plan.
    emf.setPackagesToScan(
        "com.example.multi_tanent.tenant.base.entity",
        "com.example.multi_tanent.tenant.employee.entity",
        "com.example.multi_tanent.tenant.attendance.entity",
        "com.example.multi_tanent.tenant.leave.entity",
        "com.example.multi_tanent.tenant.payroll.entity",
        "com.example.multi_tanent.tenant.recruitment.entity",
        "com.example.multi_tanent.pos.entity",
        "com.example.multi_tanent.crm.entity",
        "com.example.multi_tanent.production.entity",
        "com.example.multi_tanent.spersusers.enitity",
        "com.example.multi_tanent.sales.entity",
        "com.example.multi_tanent.purchases.entity");
    emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

    Properties p = new Properties();
    // DO NOT touch DB at boot
    p.put("hibernate.hbm2ddl.auto", "none"); // or "validate" later
    // Prevent JDBC metadata access at boot (Hibernate 6)
    p.put("hibernate.boot.allow_jdbc_metadata_access", "false");
    // Lock dialect (so Hibernate doesn't try to detect it)
    p.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
    p.put("hibernate.globally_quoted_identifiers", "true");
    p.put("hibernate.show_sql", "true");
    p.put("hibernate.format_sql", "true");
    emf.setJpaProperties(p);
    return emf;
  }

  @Bean(name = "tenantTx")
  public org.springframework.orm.jpa.JpaTransactionManager tenantTx(
      @Qualifier("tenantEmf") jakarta.persistence.EntityManagerFactory emf) {
    return new org.springframework.orm.jpa.JpaTransactionManager(emf);
  }
}
