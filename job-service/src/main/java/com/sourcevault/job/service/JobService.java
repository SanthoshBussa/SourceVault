package com.sourcevault.job.service;

import com.sourcevault.analysis.service.WebsiteAnalysisService;
import com.sourcevault.archive.service.ArchiveService;
import com.sourcevault.collection.service.ResourceCollectionService;
import com.sourcevault.collection.service.ResourceStorageService;
import com.sourcevault.common.dto.*;
import com.sourcevault.common.model.CollectionMode;
import com.sourcevault.common.model.JobStatus;
import com.sourcevault.common.model.ResourceStatus;
import com.sourcevault.common.model.ResourceType;
import com.sourcevault.common.security.SsrfValidator;
import com.sourcevault.job.entity.ArchiveRecordEntity;
import com.sourcevault.job.entity.CollectedResourceEntity;
import com.sourcevault.job.entity.CollectionJobEntity;
import com.sourcevault.job.repository.ArchiveRecordRepository;
import com.sourcevault.job.repository.CollectedResourceRepository;
import com.sourcevault.job.repository.CollectionJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private final CollectionJobRepository jobRepository;
    private final CollectedResourceRepository resourceRepository;
    private final ArchiveRecordRepository archiveRecordRepository;

    private final WebsiteAnalysisService analysisService;
    private final ResourceCollectionService collectionService;
    private final ArchiveService archiveService;
    private final ResourceStorageService storageService;
    private final SsrfValidator ssrfValidator = new SsrfValidator();

    public JobService(CollectionJobRepository jobRepository,
                      CollectedResourceRepository resourceRepository,
                      ArchiveRecordRepository archiveRecordRepository,
                      WebsiteAnalysisService analysisService,
                      ResourceCollectionService collectionService,
                      ArchiveService archiveService,
                      ResourceStorageService storageService) {
        this.jobRepository = jobRepository;
        this.resourceRepository = resourceRepository;
        this.archiveRecordRepository = archiveRecordRepository;
        this.analysisService = analysisService;
        this.collectionService = collectionService;
        this.archiveService = archiveService;
        this.storageService = storageService;
    }

    public JobResponse createAndStartJob(CreateJobRequest request, List<BrowserResourceDto> browserResources) {
        String rawUrl = request.getWebsiteUrl();
        URI uri = ssrfValidator.validateAndNormalize(rawUrl);
        String normalizedUrl = uri.toString();
        String host = uri.getHost();
        String origin = (uri.getScheme() != null ? uri.getScheme() : "https") + "://" + host +
                (uri.getPort() != -1 ? ":" + uri.getPort() : "");

        String jobId = "job_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String jobName = host + "_backup_" + jobId.substring(4, 10);

        CollectionJobEntity entity = new CollectionJobEntity();
        entity.setId(jobId);
        entity.setJobName(jobName);
        entity.setWebsiteUrl(normalizedUrl);
        entity.setWebsiteOrigin(origin);
        entity.setCollectionMode(request.getCollectionMode() != null ? request.getCollectionMode() : CollectionMode.URL_ONLY);
        entity.setStatus(JobStatus.PENDING);
        entity.setCreatedAt(Instant.now());
        entity.setStartedAt(Instant.now());

        jobRepository.save(entity);

        // Retrieve or perform analysis
        List<DiscoveredResourceDto> discovered = new ArrayList<>();
        if (request.getAnalysisId() != null) {
            analysisService.getAnalysis(request.getAnalysisId()).ifPresent(a -> {
                discovered.addAll(a.getDiscoveredResources());
                entity.setTotalDiscovered(a.getTotalDiscovered());
            });
        }

        if (discovered.isEmpty()) {
            AnalysisResponse autoAnalysis = analysisService.analyzeWebsite(new AnalysisRequest(normalizedUrl));
            discovered.addAll(autoAnalysis.getDiscoveredResources());
            entity.setTotalDiscovered(autoAnalysis.getTotalDiscovered());
        }

        entity.setStatus(JobStatus.COLLECTING);
        jobRepository.save(entity);

        // Start asynchronous collection
        collectionService.startCollection(jobId, normalizedUrl, origin, request, discovered, browserResources);

        // Run orchestration thread in background
        CompletableFuture.runAsync(() -> monitorAndArchiveJob(jobId, normalizedUrl, origin));

        return toJobResponse(entity);
    }

    private void monitorAndArchiveJob(String jobId, String websiteUrl, String origin) {
        try {
            boolean active = true;
            while (active) {
                Thread.sleep(1000);
                JobProgressDto progress = collectionService.getProgress(jobId);
                if (progress == null) {
                    break;
                }

                if (progress.getStatus() == JobStatus.COMPLETED ||
                    progress.getStatus() == JobStatus.COMPLETED_WITH_WARNINGS ||
                    progress.getStatus() == JobStatus.FAILED ||
                    progress.getStatus() == JobStatus.CANCELLED) {
                    active = false;
                }
            }

            CollectionJobEntity entity = jobRepository.findById(jobId).orElse(null);
            if (entity == null) return;

            JobProgressDto finalProgress = collectionService.getProgress(jobId);
            List<ResourceDto> collectedList = collectionService.getResources(jobId);

            if (finalProgress != null && finalProgress.getStatus() == JobStatus.CANCELLED) {
                entity.setStatus(JobStatus.CANCELLED);
                entity.setCompletedAt(Instant.now());
                persistCollectedResources(jobId, collectedList);
                jobRepository.save(entity);
                return;
            }

            if (finalProgress != null && finalProgress.getStatus() == JobStatus.FAILED) {
                entity.setStatus(JobStatus.FAILED);
                entity.setCompletedAt(Instant.now());
                entity.setErrorMessage("Resource collection encountered critical failures.");
                persistCollectedResources(jobId, collectedList);
                jobRepository.save(entity);
                return;
            }

            // Step: Archiving
            entity.setStatus(JobStatus.ARCHIVING);
            jobRepository.save(entity);

            ArchiveService.ArchiveMetadata archiveMeta = archiveService.createArchive(
                    jobId,
                    websiteUrl,
                    collectedList,
                    checksum -> {
                        try {
                            return storageService.readResource(jobId, checksum);
                        } catch (Exception e) {
                            log.error("Could not read resource checksum {}", checksum, e);
                            return null;
                        }
                    }
            );

            // Persist Archive Record
            ArchiveRecordEntity archiveEntity = new ArchiveRecordEntity();
            archiveEntity.setId(UUID.randomUUID().toString());
            archiveEntity.setJobId(jobId);
            archiveEntity.setArchiveFileName(archiveMeta.getArchiveFileName());
            archiveEntity.setArchivePath(archiveMeta.getArchivePath().toString());
            archiveEntity.setArchiveSize(archiveMeta.getArchiveSize());
            archiveEntity.setChecksum(archiveMeta.getChecksumSha256());
            archiveEntity.setValidationStatus(archiveMeta.getValidationStatus());
            archiveEntity.setCreatedAt(Instant.now());
            archiveRecordRepository.save(archiveEntity);

            // Update Job Entity
            entity.setStatus("VALID".equalsIgnoreCase(archiveMeta.getValidationStatus())
                    ? JobStatus.COMPLETED
                    : JobStatus.COMPLETED_WITH_WARNINGS);
            entity.setCompletedAt(Instant.now());
            entity.setTotalDownloaded(finalProgress != null ? finalProgress.getTotalDownloaded() : 0);
            entity.setTotalFailed(finalProgress != null ? finalProgress.getTotalFailed() : 0);
            entity.setTotalSkipped(finalProgress != null ? finalProgress.getTotalSkipped() : 0);
            entity.setTotalReconstructed(archiveMeta.getReconstructedCount());
            entity.setTotalBytes(archiveMeta.getArchiveSize());
            entity.setArchivePath(archiveMeta.getArchivePath().toString());
            entity.setArchiveFileName(archiveMeta.getArchiveFileName());
            entity.setArchiveSize(archiveMeta.getArchiveSize());
            entity.setValidationStatus(archiveMeta.getValidationStatus());

            persistCollectedResources(jobId, collectedList);
            jobRepository.save(entity);
            log.info("Job {} completed successfully with archive: {}", jobId, archiveMeta.getArchiveFileName());

        } catch (Exception e) {
            log.error("Error archiving job {}", jobId, e);
            jobRepository.findById(jobId).ifPresent(entity -> {
                entity.setStatus(JobStatus.FAILED);
                entity.setErrorMessage("Archive packaging failed: " + e.getMessage());
                entity.setCompletedAt(Instant.now());
                jobRepository.save(entity);
            });
        }
    }

    private void persistCollectedResources(String jobId, List<ResourceDto> resources) {
        if (resources == null || resources.isEmpty()) return;
        List<CollectedResourceEntity> entities = new ArrayList<>();
        for (ResourceDto dto : resources) {
            CollectedResourceEntity res = new CollectedResourceEntity();
            res.setId(dto.getId() != null ? dto.getId() : UUID.randomUUID().toString());
            res.setJobId(jobId);
            res.setOriginalUrl(dto.getOriginalUrl());
            res.setResourceOrigin(dto.getResourceOrigin() != null ? dto.getResourceOrigin() : "");
            res.setResourcePath(dto.getResourcePath() != null ? dto.getResourcePath() : "/");
            res.setFileName(dto.getFileName() != null ? dto.getFileName() : "unnamed");
            res.setResourceType(dto.getResourceType() != null ? dto.getResourceType() : ResourceType.OTHER);
            res.setContentType(dto.getContentType());
            res.setHttpStatus(dto.getHttpStatus());
            res.setFileSize(dto.getFileSize() != null ? dto.getFileSize() : 0L);
            res.setChecksum(dto.getChecksum());
            res.setRetrievalMethod(dto.getRetrievalMethod());
            res.setStatus(dto.getStatus());
            res.setFailureReason(dto.getFailureReason());
            res.setReconstructed(dto.isReconstructed());
            res.setCreatedAt(dto.getCreatedAt() != null ? dto.getCreatedAt() : Instant.now());
            entities.add(res);
        }
        resourceRepository.saveAll(entities);
    }

    public Optional<JobResponse> getJob(String jobId) {
        return jobRepository.findById(jobId).map(this::toJobResponse);
    }

    public Page<JobResponse> listJobs(Pageable pageable) {
        return jobRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toJobResponse);
    }

    public Page<ResourceDto> getJobResources(String jobId, ResourceStatus status, ResourceType type, String search, Pageable pageable) {
        return resourceRepository.findFiltered(jobId, status, type, search, pageable).map(this::toResourceDto);
    }

    public JobProgressDto getJobProgress(String jobId) {
        JobProgressDto live = collectionService.getProgress(jobId);
        if (live != null) {
            return live;
        }

        return jobRepository.findById(jobId).map(job -> {
            JobProgressDto p = new JobProgressDto();
            p.setJobId(jobId);
            p.setStatus(job.getStatus());
            p.setTotalDiscovered(job.getTotalDiscovered());
            p.setTotalDownloaded(job.getTotalDownloaded());
            p.setTotalFailed(job.getTotalFailed());
            p.setTotalSkipped(job.getTotalSkipped());
            p.setTotalReconstructed(job.getTotalReconstructed());
            p.setTotalBytes(job.getTotalBytes());
            p.setPercentage(job.getStatus() == JobStatus.COMPLETED || job.getStatus() == JobStatus.COMPLETED_WITH_WARNINGS ? 100.0 : 0.0);
            return p;
        }).orElse(null);
    }

    public boolean cancelJob(String jobId) {
        boolean cancelled = collectionService.cancelJob(jobId);
        jobRepository.findById(jobId).ifPresent(entity -> {
            entity.setStatus(JobStatus.CANCELLED);
            entity.setCompletedAt(Instant.now());
            jobRepository.save(entity);
        });
        return cancelled;
    }

    @Transactional
    public void deleteJob(String jobId) {
        archiveRecordRepository.findByJobId(jobId).ifPresent(rec -> {
            try {
                Files.deleteIfExists(Path.of(rec.getArchivePath()));
            } catch (Exception ignored) {}
        });
        archiveRecordRepository.deleteByJobId(jobId);
        resourceRepository.deleteByJobId(jobId);
        jobRepository.deleteById(jobId);
        storageService.cleanJob(jobId);
    }

    private JobResponse toJobResponse(CollectionJobEntity entity) {
        JobResponse dto = new JobResponse();
        dto.setId(entity.getId());
        dto.setJobName(entity.getJobName());
        dto.setWebsiteUrl(entity.getWebsiteUrl());
        dto.setWebsiteOrigin(entity.getWebsiteOrigin());
        dto.setCollectionMode(entity.getCollectionMode());
        dto.setStatus(entity.getStatus());
        dto.setTotalDiscovered(entity.getTotalDiscovered());
        dto.setTotalDownloaded(entity.getTotalDownloaded());
        dto.setTotalFailed(entity.getTotalFailed());
        dto.setTotalSkipped(entity.getTotalSkipped());
        dto.setTotalReconstructed(entity.getTotalReconstructed());
        dto.setTotalBytes(entity.getTotalBytes());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setStartedAt(entity.getStartedAt());
        dto.setCompletedAt(entity.getCompletedAt());
        dto.setArchiveFileName(entity.getArchiveFileName());
        dto.setArchivePath(entity.getArchivePath());
        dto.setArchiveSize(entity.getArchiveSize());
        dto.setValidationStatus(entity.getValidationStatus());
        dto.setErrorMessage(entity.getErrorMessage());
        return dto;
    }

    private ResourceDto toResourceDto(CollectedResourceEntity entity) {
        ResourceDto dto = new ResourceDto();
        dto.setId(entity.getId());
        dto.setJobId(entity.getJobId());
        dto.setOriginalUrl(entity.getOriginalUrl());
        dto.setResourceOrigin(entity.getResourceOrigin());
        dto.setResourcePath(entity.getResourcePath());
        dto.setFileName(entity.getFileName());
        dto.setResourceType(entity.getResourceType());
        dto.setContentType(entity.getContentType());
        dto.setHttpStatus(entity.getHttpStatus());
        dto.setFileSize(entity.getFileSize());
        dto.setChecksum(entity.getChecksum());
        dto.setRetrievalMethod(entity.getRetrievalMethod());
        dto.setStatus(entity.getStatus());
        dto.setFailureReason(entity.getFailureReason());
        dto.setReconstructed(entity.isReconstructed());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
