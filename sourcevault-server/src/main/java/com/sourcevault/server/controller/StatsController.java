package com.sourcevault.server.controller;

import com.sourcevault.common.model.JobStatus;
import com.sourcevault.gateway.browser.BrowserSessionRegistry;
import com.sourcevault.job.entity.CollectionJobEntity;
import com.sourcevault.job.repository.ArchiveRecordRepository;
import com.sourcevault.job.repository.CollectionJobRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stats")
@CrossOrigin(origins = "*")
public class StatsController {

    private final CollectionJobRepository jobRepository;
    private final ArchiveRecordRepository archiveRecordRepository;
    private final BrowserSessionRegistry browserSessionRegistry;

    public StatsController(CollectionJobRepository jobRepository,
                           ArchiveRecordRepository archiveRecordRepository,
                           BrowserSessionRegistry browserSessionRegistry) {
        this.jobRepository = jobRepository;
        this.archiveRecordRepository = archiveRecordRepository;
        this.browserSessionRegistry = browserSessionRegistry;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getStats() {
        List<CollectionJobEntity> jobs = jobRepository.findAll();
        long totalJobs = jobs.size();
        long totalDownloaded = jobs.stream().mapToLong(CollectionJobEntity::getTotalDownloaded).sum();
        long totalArchives = archiveRecordRepository.count();
        long totalStorageBytes = jobs.stream().mapToLong(CollectionJobEntity::getArchiveSize).sum();
        long activeJobs = jobs.stream().filter(j -> j.getStatus() == JobStatus.COLLECTING || j.getStatus() == JobStatus.ARCHIVING).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalJobs", totalJobs);
        stats.put("totalDownloaded", totalDownloaded);
        stats.put("totalArchives", totalArchives);
        stats.put("totalStorageBytes", totalStorageBytes);
        stats.put("activeJobs", activeJobs);
        stats.put("browserStatus", browserSessionRegistry.getStatus());

        return ResponseEntity.ok(stats);
    }
}
