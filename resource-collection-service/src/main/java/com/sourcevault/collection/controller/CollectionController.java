package com.sourcevault.collection.controller;

import com.sourcevault.collection.service.ResourceCollectionService;
import com.sourcevault.collection.service.ResourceStorageService;
import com.sourcevault.common.dto.JobProgressDto;
import com.sourcevault.common.dto.ResourceDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/collection")
@CrossOrigin(origins = "*")
public class CollectionController {

    private final ResourceCollectionService collectionService;
    private final ResourceStorageService storageService;

    public CollectionController(ResourceCollectionService collectionService, ResourceStorageService storageService) {
        this.collectionService = collectionService;
        this.storageService = storageService;
    }

    @GetMapping("/{jobId}/progress")
    public ResponseEntity<JobProgressDto> getProgress(@PathVariable String jobId) {
        JobProgressDto progress = collectionService.getProgress(jobId);
        if (progress == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(progress);
    }

    @GetMapping("/{jobId}/resources")
    public ResponseEntity<List<ResourceDto>> getResources(@PathVariable String jobId) {
        return ResponseEntity.ok(collectionService.getResources(jobId));
    }

    @PostMapping("/{jobId}/cancel")
    public ResponseEntity<Void> cancelJob(@PathVariable String jobId) {
        boolean cancelled = collectionService.cancelJob(jobId);
        return cancelled ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/{jobId}/data/{checksum}")
    public ResponseEntity<byte[]> getResourceData(@PathVariable String jobId, @PathVariable String checksum) {
        try {
            byte[] data = storageService.readResource(jobId, checksum);
            if (data == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + checksum + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
