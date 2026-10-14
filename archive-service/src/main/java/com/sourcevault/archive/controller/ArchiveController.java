package com.sourcevault.archive.controller;

import com.sourcevault.archive.service.ArchiveService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/jobs/{jobId}")
@CrossOrigin(origins = "*")
public class ArchiveController {

    private final ArchiveService archiveService;

    public ArchiveController(ArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @GetMapping("/archive")
    public ResponseEntity<Resource> downloadArchive(@PathVariable String jobId) {
        Optional<ArchiveService.ArchiveMetadata> metaOpt = archiveService.getArchiveMetadata(jobId);
        if (metaOpt.isEmpty() || !Files.exists(metaOpt.get().getArchivePath())) {
            return ResponseEntity.notFound().build();
        }

        ArchiveService.ArchiveMetadata meta = metaOpt.get();
        Resource resource = new FileSystemResource(meta.getArchivePath());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + meta.getArchiveFileName() + "\"")
                .header("X-Archive-Validation", meta.getValidationStatus())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(meta.getArchiveSize())
                .body(resource);
    }

    @GetMapping("/manifest")
    public ResponseEntity<String> getManifest(@PathVariable String jobId) {
        return archiveService.getArchiveMetadata(jobId)
                .map(m -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"manifest_" + jobId + ".csv\"")
                        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                        .body(m.getManifestCsv()))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/failed-resources")
    public ResponseEntity<String> getFailedResources(@PathVariable String jobId) {
        return archiveService.getArchiveMetadata(jobId)
                .map(m -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"failed_resources_" + jobId + ".txt\"")
                        .contentType(MediaType.TEXT_PLAIN)
                        .body(m.getFailedReport()))
                .orElse(ResponseEntity.notFound().build());
    }
}
