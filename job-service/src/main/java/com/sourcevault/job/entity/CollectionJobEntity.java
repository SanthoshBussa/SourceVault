package com.sourcevault.job.entity;

import com.sourcevault.common.model.CollectionMode;
import com.sourcevault.common.model.JobStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "collection_jobs", indexes = {
        @Index(name = "idx_job_created_at", columnList = "createdAt"),
        @Index(name = "idx_job_status", columnList = "status")
})
public class CollectionJobEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false)
    private String jobName;

    @Column(nullable = false, length = 2048)
    private String websiteUrl;

    @Column(nullable = false, length = 512)
    private String websiteOrigin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CollectionMode collectionMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    private int totalDiscovered;
    private int totalDownloaded;
    private int totalFailed;
    private int totalSkipped;
    private int totalReconstructed;
    private long totalBytes;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant startedAt;
    private Instant completedAt;

    @Column(length = 1024)
    private String archivePath;

    @Column(length = 256)
    private String archiveFileName;

    private long archiveSize;

    @Column(length = 64)
    private String validationStatus;

    @Column(length = 2048)
    private String errorMessage;

    public CollectionJobEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }
    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }
    public String getWebsiteOrigin() { return websiteOrigin; }
    public void setWebsiteOrigin(String websiteOrigin) { this.websiteOrigin = websiteOrigin; }
    public CollectionMode getCollectionMode() { return collectionMode; }
    public void setCollectionMode(CollectionMode collectionMode) { this.collectionMode = collectionMode; }
    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }
    public int getTotalDiscovered() { return totalDiscovered; }
    public void setTotalDiscovered(int totalDiscovered) { this.totalDiscovered = totalDiscovered; }
    public int getTotalDownloaded() { return totalDownloaded; }
    public void setTotalDownloaded(int totalDownloaded) { this.totalDownloaded = totalDownloaded; }
    public int getTotalFailed() { return totalFailed; }
    public void setTotalFailed(int totalFailed) { this.totalFailed = totalFailed; }
    public int getTotalSkipped() { return totalSkipped; }
    public void setTotalSkipped(int totalSkipped) { this.totalSkipped = totalSkipped; }
    public int getTotalReconstructed() { return totalReconstructed; }
    public void setTotalReconstructed(int totalReconstructed) { this.totalReconstructed = totalReconstructed; }
    public long getTotalBytes() { return totalBytes; }
    public void setTotalBytes(long totalBytes) { this.totalBytes = totalBytes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public String getArchivePath() { return archivePath; }
    public void setArchivePath(String archivePath) { this.archivePath = archivePath; }
    public String getArchiveFileName() { return archiveFileName; }
    public void setArchiveFileName(String archiveFileName) { this.archiveFileName = archiveFileName; }
    public long getArchiveSize() { return archiveSize; }
    public void setArchiveSize(long archiveSize) { this.archiveSize = archiveSize; }
    public String getValidationStatus() { return validationStatus; }
    public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
