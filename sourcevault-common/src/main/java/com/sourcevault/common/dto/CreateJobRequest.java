package com.sourcevault.common.dto;

import com.sourcevault.common.model.CollectionMode;
import jakarta.validation.constraints.NotBlank;

public class CreateJobRequest {
    private String analysisId;

    @NotBlank(message = "Website URL is required")
    private String websiteUrl;

    private CollectionMode collectionMode = CollectionMode.URL_ONLY;
    private boolean includeThirdParty = false;
    private int maxResourceCount = 500;
    private long maxTotalBytes = 100 * 1024 * 1024L; // 100MB default limit
    private boolean reconstructSourceMaps = true;

    public CreateJobRequest() {}

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public CollectionMode getCollectionMode() {
        return collectionMode;
    }

    public void setCollectionMode(CollectionMode collectionMode) {
        this.collectionMode = collectionMode;
    }

    public boolean isIncludeThirdParty() {
        return includeThirdParty;
    }

    public void setIncludeThirdParty(boolean includeThirdParty) {
        this.includeThirdParty = includeThirdParty;
    }

    public int getMaxResourceCount() {
        return maxResourceCount;
    }

    public void setMaxResourceCount(int maxResourceCount) {
        this.maxResourceCount = maxResourceCount;
    }

    public long getMaxTotalBytes() {
        return maxTotalBytes;
    }

    public void setMaxTotalBytes(long maxTotalBytes) {
        this.maxTotalBytes = maxTotalBytes;
    }

    public boolean isReconstructSourceMaps() {
        return reconstructSourceMaps;
    }

    public void setReconstructSourceMaps(boolean reconstructSourceMaps) {
        this.reconstructSourceMaps = reconstructSourceMaps;
    }
}
