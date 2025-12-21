package com.example.multi_tanent.tenant.payroll.dto;

import com.example.multi_tanent.spersusers.enitity.CompanyLocation;

import lombok.Data;

@Data
public class CompanyLocationResponse {
    private Long id;
    private String locationName;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private boolean isPrimary;

    public static CompanyLocationResponse fromEntity(CompanyLocation location) {
        if (location == null)
            return null;
        CompanyLocationResponse dto = new CompanyLocationResponse();
        dto.setId(location.getId());
        dto.setLocationName(location.getLocationName());
        dto.setAddress(location.getAddress());
        dto.setCity(location.getCity());
        dto.setState(location.getState());
        dto.setPostalCode(location.getPostalCode());
        dto.setCountry(location.getCountry());
        dto.setPrimary(location.isPrimary());
        return dto;
    }
}