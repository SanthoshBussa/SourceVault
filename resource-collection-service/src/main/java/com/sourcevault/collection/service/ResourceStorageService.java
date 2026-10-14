package com.sourcevault.collection.service;

import com.sourcevault.common.security.PathSanitizer;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ResourceStorageService {

    private static final Logger log = LoggerFactory.getLogger(ResourceStorageService.class);

    private final Path baseStorageDir;

    public ResourceStorageService(@Value("${sourcevault.collection.storage-dir:./data/collected-resources}") String storageDir) {
        this.baseStorageDir = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseStorageDir);
        } catch (IOException e) {
            log.error("Could not create storage directory: {}", this.baseStorageDir, e);
        }
    }

    public Path getJobDirectory(String jobId) throws IOException {
        Path jobDir = this.baseStorageDir.resolve(PathSanitizer.sanitizeFilename(jobId)).normalize();
        Files.createDirectories(jobDir);
        return jobDir;
    }

    public Path storeResource(String jobId, String resourceKey, byte[] data) throws IOException {
        Path jobDir = getJobDirectory(jobId);
        String safeName = PathSanitizer.sanitizeFilename(resourceKey);
        Path targetFile = jobDir.resolve(safeName);
        Files.write(targetFile, data);
        return targetFile;
    }

    public byte[] readResource(String jobId, String resourceKey) throws IOException {
        Path jobDir = getJobDirectory(jobId);
        String safeName = PathSanitizer.sanitizeFilename(resourceKey);
        Path targetFile = jobDir.resolve(safeName);
        if (Files.exists(targetFile)) {
            return Files.readAllBytes(targetFile);
        }
        return null;
    }

    public void cleanJob(String jobId) {
        try {
            Path jobDir = this.baseStorageDir.resolve(PathSanitizer.sanitizeFilename(jobId)).normalize();
            if (Files.exists(jobDir)) {
                FileUtils.deleteDirectory(jobDir.toFile());
            }
        } catch (Exception e) {
            log.warn("Could not clean job directory for {}: {}", jobId, e.getMessage());
        }
    }
}
