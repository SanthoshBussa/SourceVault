package com.sourcevault.common.dto;

import com.sourcevault.common.model.ResourceStatus;
import com.sourcevault.common.model.ResourceType;
import com.sourcevault.common.model.RetrievalMethod;

import java.time.Instant;

public class ResourceDto {
    private String id;
    private String jobId;
    private String originalUrl;
    private String resourceOrigin;
    private String resourcePath;
    private String fileName;
    private ResourceType resourceType;
    private String contentType;
    private Integer httpStatus;
    private Long fileSize;
    private String checksum;
    private RetrievalMethod retrievalMethod;
    private ResourceStatus status;
    private String failureReason;
    private boolean isReconstructed;
    private Instant createdAt;

    public ResourceDto() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getResourceOrigin() {
        return resourceOrigin;
    }

    public void setResourceOrigin(String resourceOrigin) {
        this.resourceOrigin = resourceOrigin;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public RetrievalMethod getRetrievalMethod() {
        return retrievalMethod;
    }

    public void setRetrievalMethod(RetrievalMethod retrievalMethod) {
        this.retrievalMethod = retrievalMethod;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public boolean isReconstructed() {
        return isReconstructed;
    }

    public void setReconstructed(boolean reconstructed) {
        isReconstructed = reconstructed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
