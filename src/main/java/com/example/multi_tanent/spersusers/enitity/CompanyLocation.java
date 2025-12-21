package com.example.multi_tanent.spersusers.enitity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "company_locations")
@Data
public class CompanyLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_info_id", nullable = false)
    @JsonIgnore // Avoid serialization loops
    @ToString.Exclude // Avoid recursion in toString()
    @EqualsAndHashCode.Exclude // Avoid recursion in equals/hashCode
    private CompanyInfo companyInfo;

    @Column(nullable = false)
    private String locationName; // e.g., "Head Office", "Mumbai Branch"

    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    private boolean isPrimary; // To mark the main/head office
}