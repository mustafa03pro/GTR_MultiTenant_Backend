package com.example.multi_tanent.master.service;

import com.example.multi_tanent.master.entity.MasterUser;
import com.example.multi_tanent.master.enums.Role;
import com.example.multi_tanent.master.repository.MasterUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class MasterDataService {

    private static final Logger logger = LoggerFactory.getLogger(MasterDataService.class);

    private final MasterUserRepository masterUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.default-master-admin.username:masteradmin}")
    private String defaultUsername;

    @Value("${app.default-master-admin.password:password123}")
    private String defaultPassword;

    public MasterDataService(MasterUserRepository masterUserRepository, PasswordEncoder passwordEncoder) {
        this.masterUserRepository = masterUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional("masterTx")
    public void initMasterAdmin() {
        logger.info("DEBUG: Checking existing users in database:");
        masterUserRepository.findAll().forEach(u -> logger.info("DEBUG: Found user: {}", u.getUsername()));

        if (masterUserRepository.findByUsername(defaultUsername).isEmpty()) {
            logger.info("Default master admin user not found. Creating user '{}'.", defaultUsername);
            MasterUser masterUser = new MasterUser();
            masterUser.setUsername(defaultUsername);
            masterUser.setPasswordHash(passwordEncoder.encode(defaultPassword));
            masterUser.setRoles(Set.of(Role.MASTER_ADMIN)); // Assign the role
            masterUserRepository.saveAndFlush(masterUser); // Use saveAndFlush to enforce DB write
            logger.info("============================================================");
            logger.info("Default Master Admin created. Username: {}, Password: {}", defaultUsername,
                    defaultPassword);
            logger.info("============================================================");

            // Verification step
            if (masterUserRepository.findByUsername(defaultUsername).isPresent()) {
                logger.info("VERIFICATION SUCCESS: User '{}' was saved and can be read back from the database.",
                        defaultUsername);
            } else {
                logger.error("VERIFICATION FAILED: User '{}' was saved but cannot be found immediately!",
                        defaultUsername);
            }
        }
    }
}
