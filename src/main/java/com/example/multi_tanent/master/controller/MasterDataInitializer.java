package com.example.multi_tanent.master.controller;

import com.example.multi_tanent.master.service.MasterDataService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MasterDataInitializer {

    @Bean
    public ApplicationRunner initializeMasterAdmin(MasterDataService masterDataService) {
        return args -> {
            masterDataService.initMasterAdmin();
        };
    }
}