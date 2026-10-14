package com.sourcevault.common.dto;

import com.sourcevault.common.model.CollectionMode;
import com.sourcevault.common.model.JobStatus;

import java.time.Instant;

public class JobResponse {
    private String id;
    private String jobName;
    private String websiteUrl;
    private String websiteOrigin;
    private CollectionMode collectionMode;
    private JobStatus status;
    private int totalDiscovered;
    private int totalDownloaded;
    private int totalFailed;
    private int totalSkipped;
    private int totalReconstructed;
    private long totalBytes;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private String archiveFileName;
    private String archivePath;
    private long archiveSize;
    private String validationStatus;
    private String errorMessage;

    public JobResponse() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public String getWebsiteOrigin() {
        return websiteOrigin;
    }

    public void setWebsiteOrigin(String websiteOrigin) {
        this.websiteOrigin = websiteOrigin;
    }

    public CollectionMode getCollectionMode() {
        return collectionMode;
    }

    public void setCollectionMode(CollectionMode collectionMode) {
        this.collectionMode = collectionMode;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public int getTotalDiscovered() {
        return totalDiscovered;
    }

    public void setTotalDiscovered(int totalDiscovered) {
        this.totalDiscovered = totalDiscovered;
    }

    public int getTotalDownloaded() {
        return totalDownloaded;
    }

    public void setTotalDownloaded(int totalDownloaded) {
        this.totalDownloaded = totalDownloaded;
    }

    public int getTotalFailed() {
        return totalFailed;
    }

    public void setTotalFailed(int totalFailed) {
        this.totalFailed = totalFailed;
    }

    public int getTotalSkipped() {
        return totalSkipped;
    }

    public void setTotalSkipped(int totalSkipped) {
        this.totalSkipped = totalSkipped;
    }

    public int getTotalReconstructed() {
        return totalReconstructed;
    }

    public void setTotalReconstructed(int totalReconstructed) {
        this.totalReconstructed = totalReconstructed;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getArchiveFileName() {
        return archiveFileName;
    }

    public void setArchiveFileName(String archiveFileName) {
        this.archiveFileName = archiveFileName;
    }

    public String getArchivePath() {
        return archivePath;
    }

    public void setArchivePath(String archivePath) {
        this.archivePath = archivePath;
    }

    public long getArchiveSize() {
        return archiveSize;
    }

    public void setArchiveSize(long archiveSize) {
        this.archiveSize = archiveSize;
    }

    public String getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(String validationStatus) {
        this.validationStatus = validationStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
