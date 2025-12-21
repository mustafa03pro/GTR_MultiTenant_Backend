package com.example.multi_tanent.tenant.payroll.service;

import com.example.multi_tanent.spersusers.enitity.CompanyInfo;
import com.example.multi_tanent.spersusers.enitity.CompanyLocation;
import com.example.multi_tanent.tenant.payroll.dto.CompanyLocationRequest;
import com.example.multi_tanent.tenant.payroll.repository.CompanyInfoRepository;
import com.example.multi_tanent.tenant.payroll.repository.CompanyLocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(transactionManager = "tenantTx")
public class CompanyLocationService {

    private final CompanyLocationRepository locationRepository;
    private final CompanyInfoRepository companyInfoRepository;

    public CompanyLocationService(CompanyLocationRepository locationRepository,
            CompanyInfoRepository companyInfoRepository) {
        this.locationRepository = locationRepository;
        this.companyInfoRepository = companyInfoRepository;
    }

    public List<CompanyLocation> getAllLocations() {
        return locationRepository.findAll();
    }

    public CompanyLocation createLocation(CompanyLocationRequest request) {
        CompanyInfo companyInfo = companyInfoRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "CompanyInfo not found for this tenant. Please create it first."));

        CompanyLocation location = new CompanyLocation();
        mapRequestToEntity(request, location);
        location.setCompanyInfo(companyInfo);
        return locationRepository.save(location);
    }

    public CompanyLocation updateLocation(Long id, CompanyLocationRequest request) {
        CompanyLocation location = locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("CompanyLocation not found with id: " + id));
        mapRequestToEntity(request, location);
        return locationRepository.save(location);
    }

    public void deleteLocation(Long id) {
        locationRepository.deleteById(id);
    }

    private void mapRequestToEntity(CompanyLocationRequest req, CompanyLocation entity) {
        entity.setLocationName(req.getLocationName());
        entity.setAddress(req.getAddress());
        entity.setCity(req.getCity());
        entity.setState(req.getState());
        entity.setPostalCode(req.getPostalCode());
        entity.setCountry(req.getCountry());
        entity.setPrimary(req.isPrimary());
    }
}