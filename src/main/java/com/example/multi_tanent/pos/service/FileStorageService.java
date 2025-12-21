package com.example.multi_tanent.pos.service;

import com.example.multi_tanent.config.TenantContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service("posFileStorageService")
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService(@Value("${file.upload-dir:./uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file, String subfolder) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            if (originalFileName.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + originalFileName);
            }

            String fileExtension = "";
            try {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            } catch (Exception e) {
                // ignore
            }

            String tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                throw new IllegalStateException("Tenant context is not set. Cannot upload file.");
            }

            String newFileName = UUID.randomUUID().toString() + fileExtension;

            Path targetLocation = this.fileStorageLocation.resolve(tenantId).resolve(subfolder);
            Files.createDirectories(targetLocation);
            Path filePath = targetLocation.resolve(newFileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return Paths.get(tenantId, subfolder, newFileName).toString().replace("\\", "/");
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFileName + ". Please try again!", ex);
        }
    }

    public String storeFile(byte[] fileBytes, String originalFileName, String subfolder) {
        String cleanFileName = StringUtils.cleanPath(originalFileName);

        try {
            if (cleanFileName.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + cleanFileName);
            }

            String fileExtension = "";
            try {
                fileExtension = cleanFileName.substring(cleanFileName.lastIndexOf("."));
            } catch (Exception e) {
                // ignore
            }

            String tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                throw new IllegalStateException("Tenant context is not set. Cannot upload file.");
            }

            String newFileName = UUID.randomUUID().toString() + fileExtension;

            Path targetLocation = this.fileStorageLocation.resolve(tenantId).resolve(subfolder);
            Files.createDirectories(targetLocation);
            Path filePath = targetLocation.resolve(newFileName);
            Files.copy(new ByteArrayInputStream(fileBytes), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Return the relative path including tenant, subfolder, and filename
            return Paths.get(tenantId, subfolder, newFileName).toString().replace("\\", "/");

        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + cleanFileName + ". Please try again!", ex);
        }
    }

    public String storeFileWithCustomName(byte[] fileBytes, String customFileName, String subfolder) {
        String cleanFileName = StringUtils.cleanPath(customFileName);

        try {
            if (cleanFileName.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + cleanFileName);
            }

            String tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                throw new IllegalStateException("Tenant context is not set. Cannot upload file.");
            }

            Path targetLocation = this.fileStorageLocation.resolve(tenantId).resolve(subfolder);
            Files.createDirectories(targetLocation);
            Path filePath = targetLocation.resolve(cleanFileName);
            Files.copy(new ByteArrayInputStream(fileBytes), filePath, StandardCopyOption.REPLACE_EXISTING);

            return Paths.get(tenantId, subfolder, cleanFileName).toString().replace("\\", "/");
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + cleanFileName + ". Please try again!", ex);
        }
    }

    /**
     * Stores a file in a public location that is not tied to a specific tenant's
     * subfolder.
     * This is useful for shared assets like tenant logos.
     *
     * @param fileBytes        The byte array of the file to store.
     * @param originalFileName The original name of the file.
     * @param subfolder        The public subfolder to store the file in (e.g.,
     *                         "tenant-assets/logos").
     * @return The relative path to the stored file.
     */
    public String storePublicFile(byte[] fileBytes, String originalFileName, String subfolder) {
        String cleanFileName = StringUtils.cleanPath(originalFileName);
        try {
            if (cleanFileName.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + cleanFileName);
            }

            String fileExtension = "";
            try {
                fileExtension = cleanFileName.substring(cleanFileName.lastIndexOf("."));
            } catch (Exception e) {
                // ignore
            }

            String newFileName = UUID.randomUUID().toString() + fileExtension;

            Path targetLocation = this.fileStorageLocation.resolve(subfolder);
            Files.createDirectories(targetLocation);
            Path filePath = targetLocation.resolve(newFileName);
            Files.copy(new ByteArrayInputStream(fileBytes), filePath, StandardCopyOption.REPLACE_EXISTING);

            return Paths.get(subfolder, newFileName).toString().replace("\\", "/");
        } catch (IOException ex) {
            throw new RuntimeException("Could not store public file " + cleanFileName + ". Please try again!", ex);
        }
    }

    public Resource loadFileAsResource(String filePath) {
        try {
            Path resolvedPath = this.fileStorageLocation.resolve(filePath).normalize();
            Resource resource = new UrlResource(resolvedPath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found " + filePath);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found " + filePath, ex);
        }
    }
}