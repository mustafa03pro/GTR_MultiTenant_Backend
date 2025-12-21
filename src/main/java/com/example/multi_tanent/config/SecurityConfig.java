package com.example.multi_tanent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.multi_tanent.security.JwtAuthFilter;
import com.example.multi_tanent.security.JwtUtil;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import jakarta.servlet.ServletException;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity // enables @PreAuthorize
public class SecurityConfig {

  // @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
  // private String[] allowedOrigins;

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http, JwtUtil jwt) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .authorizeHttpRequests(auth -> auth
            // Publicly accessible endpoints
            .requestMatchers(
                "/", "/index.html",
                "/dist/**",
                "/favicon.ico",
                "/css/**", "/js/**", "/assets/**", "/images/**",
                "/static/**",
                "/uploads/**")
            .permitAll()
            .requestMatchers("/api/master/auth/login", "/api/auth/login").permitAll()
            .requestMatchers("/api/master/tenant-requests/register").permitAll()
            .requestMatchers("/api/biometric-punch/**").permitAll()
            .requestMatchers("/public/products/**").permitAll()
            .requestMatchers("/api/pos/uploads/view/**").permitAll()

            // Master Admin Endpoints
            .requestMatchers("/api/master/tenant-requests/**").authenticated()
            .requestMatchers("/api/provision").hasRole("MASTER_ADMIN")
            .requestMatchers("/api/master/tenants/**").hasRole("MASTER_ADMIN")
            .requestMatchers("/api/master/users/**").authenticated()

            // Shared/Base Tenant Endpoints
            .requestMatchers("/api/users/**").authenticated()
            .requestMatchers("/api/locations/**").authenticated()
            .requestMatchers("/api/parties/**").authenticated()

            // HRMS Module Endpoints
            .requestMatchers("/api/base/**", "/api/employees/**", "/api/job-details/**").authenticated()
            .requestMatchers("/api/employee-profiles/**", "/api/employee-documents/**").authenticated()
            // Attendance
            .requestMatchers("/api/attendance-records/**", "/api/attendance-missing/**", "/api/time-attendence/**")
            .authenticated()
            .requestMatchers("/api/attendance-policies/**", "/api/shift-policies/**", "/api/weekly-off-policies/**")
            .authenticated()
            // Leave
            .requestMatchers("/api/leaves/**", "/api/leave-requests/**", "/api/leave-balances/**",
                "/api/leave-policies/**")
            .authenticated()
            // Payroll, Benefits, and EOS
            .requestMatchers("/api/payroll-runs/**", "/api/payslips/**", "/api/payroll-settings/**").authenticated()
            .requestMatchers("/api/salary-components/**", "/api/salary-structures/**").authenticated()
            .requestMatchers("/api/benefit-types/**", "/api/provisions/**").authenticated() // Added new benefit
                                                                                            // endpoints
            .requestMatchers("/api/eos/**").authenticated() // Added End of Service endpoint
            .requestMatchers("/api/loan-products/**", "/api/employee-loans/**", "/api/expenses/**").authenticated()
            // Company and Admin
            .requestMatchers("/api/company-info/**", "/api/company-locations/**", "/api/company-bank-accounts/**")
            .authenticated()
            .requestMatchers("/api/admin/**").authenticated()

            // POS Module Endpoints
            .requestMatchers("/api/pos/**").authenticated()

            // CRM Module Endpoints
            .requestMatchers("/api/crm/**").authenticated()
            .requestMatchers("/api/contacts/**").authenticated()

            // Sales Module Endpoints
            .requestMatchers("/api/sales/rental-quotations", "/api/sales/rental-quotations/**",
                "/api/sales/rental-quotations/status-by-number", "/api/sales/rental-quotations/status/by-number")
            .permitAll()
            .requestMatchers("/api/sales/quotations/status-by-number", "/api/sales/quotations/status/by-number")
            .permitAll()
            .requestMatchers("/api/sales/**").authenticated()
            .requestMatchers("/api/sales/attachments/quotations/**").permitAll()
            .requestMatchers("/api/sales/attachments/rental_quotations/**").permitAll()
            .requestMatchers("/api/sales/attachments/rental-quotations/**").permitAll()
            .requestMatchers("/api/sales/attachments/view").permitAll()
             //purchase
             .requestMatchers("/api/purchase/**", "/api/purchases/**").hasAnyRole("SUPER_ADMIN", "PURCHASE_ADMIN")


            .anyRequest().authenticated() // Secure all other API endpoints by default
        )
        .addFilterBefore(spaRedirectFilter(), ChannelProcessingFilter.class)
        .addFilterBefore(new JwtAuthFilter(jwt), UsernamePasswordAuthenticationFilter.class)
        .sessionManagement(sm -> sm.sessionCreationPolicy(
            org.springframework.security.config.http.SessionCreationPolicy.STATELESS));
    return http.build();
  }

  @Bean
  public Filter spaRedirectFilter() {
    return (servletRequest, servletResponse, filterChain) -> {
      HttpServletRequest request = (HttpServletRequest) servletRequest;
      String path = request.getRequestURI();

      // Forward to index.html if it's not an API call, not a static file, and not the
      // root
      if (!path.startsWith("/api") && !path.contains(".") && !path.equals("/")) {
        request.getRequestDispatcher("/index.html").forward(servletRequest, servletResponse);
        return;
      }

      filterChain.doFilter(servletRequest, servletResponse);
    };
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(
        java.util.List.of("http://localhost:5173", "https://thegtrgroup.com",
            "http://localhost:8080")); // React
    // app URL
    config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    config.setAllowedHeaders(java.util.List.of("*"));
    config.setAllowCredentials(true); // If using cookies or auth headers

    config.setExposedHeaders(Arrays.asList("Content-Disposition"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
