package com.sourcevault.common.dto;

import com.sourcevault.common.model.CollectionMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalysisResponse {
    private String analysisId;
    private String normalizedUrl;
    private String websiteOrigin;
    private String title;
    private String faviconUrl;
    private boolean pageAccessible;
    private int httpStatus;
    private boolean browserConnected;
    private int totalDiscovered;
    private Map<String, Integer> typeBreakdown = new HashMap<>();
    private Map<String, Integer> originBreakdown = new HashMap<>();
    private int sourceMapCount;
    private long estimatedSizeBytes;
    private List<DiscoveredResourceDto> discoveredResources = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private CollectionMode recommendedMode = CollectionMode.URL_ONLY;

    public AnalysisResponse() {}

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getNormalizedUrl() {
        return normalizedUrl;
    }

    public void setNormalizedUrl(String normalizedUrl) {
        this.normalizedUrl = normalizedUrl;
    }

    public String getWebsiteOrigin() {
        return websiteOrigin;
    }

    public void setWebsiteOrigin(String websiteOrigin) {
        this.websiteOrigin = websiteOrigin;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFaviconUrl() {
        return faviconUrl;
    }

    public void setFaviconUrl(String faviconUrl) {
        this.faviconUrl = faviconUrl;
    }

    public boolean isPageAccessible() {
        return pageAccessible;
    }

    public void setPageAccessible(boolean pageAccessible) {
        this.pageAccessible = pageAccessible;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public boolean isBrowserConnected() {
        return browserConnected;
    }

    public void setBrowserConnected(boolean browserConnected) {
        this.browserConnected = browserConnected;
    }

    public int getTotalDiscovered() {
        return totalDiscovered;
    }

    public void setTotalDiscovered(int totalDiscovered) {
        this.totalDiscovered = totalDiscovered;
    }

    public Map<String, Integer> getTypeBreakdown() {
        return typeBreakdown;
    }

    public void setTypeBreakdown(Map<String, Integer> typeBreakdown) {
        this.typeBreakdown = typeBreakdown;
    }

    public Map<String, Integer> getOriginBreakdown() {
        return originBreakdown;
    }

    public void setOriginBreakdown(Map<String, Integer> originBreakdown) {
        this.originBreakdown = originBreakdown;
    }

    public int getSourceMapCount() {
        return sourceMapCount;
    }

    public void setSourceMapCount(int sourceMapCount) {
        this.sourceMapCount = sourceMapCount;
    }

    public long getEstimatedSizeBytes() {
        return estimatedSizeBytes;
    }

    public void setEstimatedSizeBytes(long estimatedSizeBytes) {
        this.estimatedSizeBytes = estimatedSizeBytes;
    }

    public List<DiscoveredResourceDto> getDiscoveredResources() {
        return discoveredResources;
    }

    public void setDiscoveredResources(List<DiscoveredResourceDto> discoveredResources) {
        this.discoveredResources = discoveredResources;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public CollectionMode getRecommendedMode() {
        return recommendedMode;
    }

    public void setRecommendedMode(CollectionMode recommendedMode) {
        this.recommendedMode = recommendedMode;
    }
}
