package com.sourcevault.job.entity;

import com.sourcevault.common.model.ResourceStatus;
import com.sourcevault.common.model.ResourceType;
import com.sourcevault.common.model.RetrievalMethod;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "collected_resources", indexes = {
        @Index(name = "idx_res_job_id", columnList = "jobId"),
        @Index(name = "idx_res_status", columnList = "status"),
        @Index(name = "idx_res_checksum", columnList = "checksum")
})
public class CollectedResourceEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String jobId;

    @Column(nullable = false, length = 2048)
    private String originalUrl;

    @Column(nullable = false, length = 512)
    private String resourceOrigin = "";

    @Column(nullable = false, length = 1024)
    private String resourcePath = "/";

    @Column(nullable = false, length = 256)
    private String fileName = "unnamed";

    @Enumerated(EnumType.STRING)
    private ResourceType resourceType = ResourceType.OTHER;

    @Column(length = 128)
    private String contentType;

    private Integer httpStatus;

    private Long fileSize = 0L;

    @Column(length = 128)
    private String checksum;

    @Enumerated(EnumType.STRING)
    private RetrievalMethod retrievalMethod = RetrievalMethod.HTTP_PUBLIC;

    @Enumerated(EnumType.STRING)
    private ResourceStatus status = ResourceStatus.PENDING;

    @Column(length = 1024)
    private String failureReason;

    private boolean isReconstructed;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public CollectedResourceEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }
    public String getResourceOrigin() { return resourceOrigin; }
    public void setResourceOrigin(String resourceOrigin) { this.resourceOrigin = resourceOrigin; }
    public String getResourcePath() { return resourcePath; }
    public void setResourcePath(String resourcePath) { this.resourcePath = resourcePath; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public ResourceType getResourceType() { return resourceType; }
    public void setResourceType(ResourceType resourceType) { this.resourceType = resourceType; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Integer getHttpStatus() { return httpStatus; }
    public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    public RetrievalMethod getRetrievalMethod() { return retrievalMethod; }
    public void setRetrievalMethod(RetrievalMethod retrievalMethod) { this.retrievalMethod = retrievalMethod; }
    public ResourceStatus getStatus() { return status; }
    public void setStatus(ResourceStatus status) { this.status = status; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public boolean isReconstructed() { return isReconstructed; }
    public void setReconstructed(boolean reconstructed) { isReconstructed = reconstructed; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
