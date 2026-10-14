package com.sourcevault.job.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "archive_records", indexes = {
        @Index(name = "idx_archive_job_id", columnList = "jobId")
})
public class ArchiveRecordEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String jobId;

    @Column(nullable = false, length = 256)
    private String archiveFileName;

    @Column(nullable = false, length = 1024)
    private String archivePath;

    private long archiveSize;

    @Column(length = 128)
    private String checksum;

    @Column(length = 64)
    private String validationStatus;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant expiresAt;

    public ArchiveRecordEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public String getArchiveFileName() { return archiveFileName; }
    public void setArchiveFileName(String archiveFileName) { this.archiveFileName = archiveFileName; }
    public String getArchivePath() { return archivePath; }
    public void setArchivePath(String archivePath) { this.archivePath = archivePath; }
    public long getArchiveSize() { return archiveSize; }
    public void setArchiveSize(long archiveSize) { this.archiveSize = archiveSize; }
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    public String getValidationStatus() { return validationStatus; }
    public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
