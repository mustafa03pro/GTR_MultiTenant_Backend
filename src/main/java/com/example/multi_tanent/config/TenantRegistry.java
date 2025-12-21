// com/example/multi_tanent/config/TenantRegistry.java
package com.example.multi_tanent.config;

import com.example.multi_tanent.master.entity.MasterTenant;
import com.example.multi_tanent.master.repository.MasterTenantRepository;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TenantRegistry {
  private final MasterTenantRepository masterRepo;
  private final TenantSchemaCreator schemaCreator;
  private final ConcurrentHashMap<String, DataSource> map = new ConcurrentHashMap<>();
  private TenantRoutingDataSource routing;

  public TenantRegistry(MasterTenantRepository masterRepo, TenantSchemaCreator schemaCreator) {
    this.masterRepo = masterRepo;
    this.schemaCreator = schemaCreator;
  }

  public void attachRouting(TenantRoutingDataSource routing) {
    this.routing = routing;
  }

  public void loadAllFromMaster() {
    masterRepo.findAll().forEach(this::addOrUpdateTenant);
  }

  public synchronized void addOrUpdateTenant(MasterTenant t) {
    try {
      HikariDataSource ds = new HikariDataSource();
      String originalUrl = t.getJdbcUrl();
      // Robustness: ensure we have createDatabaseIfNotExist if missing
      if (originalUrl != null && !originalUrl.contains("createDatabaseIfNotExist")) {
        if (originalUrl.contains("?")) {
          originalUrl += "&createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true";
        } else {
          originalUrl += "?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true";
        }
      }
      ds.setJdbcUrl(originalUrl);
      ds.setUsername(t.getUsername());
      ds.setPassword(t.getPassword());

      // OPTIMIZATION: Reduce pool size for low-resource environments
      // ds.setMaximumPoolSize(5); // Default is 10
      // ds.setMinimumIdle(1); // Close idle connections to save memory

      // Validate connection before adding to map (optional, but good for fail-fast)
      // ds.getConnection().close();

      map.put(t.getTenantId(), ds);

      // Ensure the schema for this tenant is up-to-date on load/reload.
      schemaCreator.ensureSchema(ds, t);
      refreshRouting();
      System.out.println("✅ Successfully loaded tenant: " + t.getTenantId());
    } catch (Exception e) {
      System.err.println("❌ Failed to load tenant '" + t.getTenantId() + "': " + e.getMessage());
      // e.printStackTrace(); // Optional: print stack trace for deeper debugging if
      // needed
    }
  }

  public synchronized void removeTenant(String tenantId) {
    DataSource ds = map.remove(tenantId);
    if (ds instanceof HikariDataSource h)
      h.close();
    refreshRouting();
  }

  public Map<Object, Object> asTargetMap() {
    return new HashMap<>(map);
  }

  private void refreshRouting() {
    if (routing == null)
      return;
    var targets = asTargetMap();
    routing.setTargetDataSources(targets);
    routing.afterPropertiesSet();
  }
}
