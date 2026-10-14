package com.sourcevault.common.dto;

import com.sourcevault.common.model.ResourceType;
import com.sourcevault.common.model.RetrievalMethod;

public class ArchiveManifestItem {
    private String originalUrl;
    private String localPath;
    private ResourceType resourceType;
    private String contentType;
    private int httpStatus;
    private long fileSize;
    private String checksum;
    private RetrievalMethod retrievalMethod;
    private boolean isReconstructed;

    public ArchiveManifestItem() {}

    public ArchiveManifestItem(String originalUrl, String localPath, ResourceType resourceType,
                               String contentType, int httpStatus, long fileSize,
                               String checksum, RetrievalMethod retrievalMethod, boolean isReconstructed) {
        this.originalUrl = originalUrl;
        this.localPath = localPath;
        this.resourceType = resourceType;
        this.contentType = contentType;
        this.httpStatus = httpStatus;
        this.fileSize = fileSize;
        this.checksum = checksum;
        this.retrievalMethod = retrievalMethod;
        this.isReconstructed = isReconstructed;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
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

    public int getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
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

    public boolean isReconstructed() {
        return isReconstructed;
    }

    public void setReconstructed(boolean reconstructed) {
        isReconstructed = reconstructed;
    }
}
