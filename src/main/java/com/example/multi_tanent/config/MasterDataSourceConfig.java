package com.example.multi_tanent.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.*;
import javax.sql.DataSource;
import com.zaxxer.hikari.HikariDataSource;

import jakarta.persistence.EntityManagerFactory;

import java.util.Properties;

@Configuration
@EnableJpaRepositories(basePackages = "com.example.multi_tanent.master.repository", entityManagerFactoryRef = "masterEmf", transactionManagerRef = "masterTx")
public class MasterDataSourceConfig {

  @Value("${master.datasource.url}")
  private String url;
  @Value("${master.datasource.username}")
  private String username;
  @Value("${master.datasource.password}")
  private String password;

  @Bean(name = "masterDataSource")
  @Primary
  public DataSource masterDataSource() {
    HikariDataSource ds = new HikariDataSource();
    System.out.println("============================================================");
    System.out.println("DEBUG: Master DataSource URL: " + url);
    System.out.println("DEBUG: Master DataSource Username: " + username);
    System.out.println("============================================================");
    ds.setJdbcUrl(url);
    ds.setUsername(username);
    ds.setPassword(password);
    return ds;
  }

  @Bean(name = "masterEmf")
  public LocalContainerEntityManagerFactoryBean masterEmf() {
    LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
    emf.setDataSource(masterDataSource());
    emf.setPackagesToScan(
        "com.example.multi_tanent.master.entity" // This is the correct package for MasterUser
    );
    emf.setJpaVendorAdapter(new org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter());
    Properties p = new Properties();
    p.put("hibernate.hbm2ddl.auto", "update");
    p.put("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
    p.put("hibernate.show_sql", "true");
    p.put("hibernate.format_sql", "true");
    emf.setJpaProperties(p);
    return emf;
  }

  @Bean(name = "masterTx")
  @Primary
  public JpaTransactionManager masterTx(@Qualifier("masterEmf") EntityManagerFactory masterEmf) {
    return new JpaTransactionManager(masterEmf);
  }
}
