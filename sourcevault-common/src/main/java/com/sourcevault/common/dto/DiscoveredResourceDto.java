package com.sourcevault.common.dto;

import com.sourcevault.common.model.ResourceType;

public class DiscoveredResourceDto {
    private String url;
    private ResourceType resourceType;
    private String origin;
    private String path;
    private String sourceMapUrl;
    private long inferredSize;
    private boolean isThirdParty;

    public DiscoveredResourceDto() {}

    public DiscoveredResourceDto(String url, ResourceType resourceType, String origin, String path) {
        this.url = url;
        this.resourceType = resourceType;
        this.origin = origin;
        this.path = path;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getSourceMapUrl() {
        return sourceMapUrl;
    }

    public void setSourceMapUrl(String sourceMapUrl) {
        this.sourceMapUrl = sourceMapUrl;
    }

    public long getInferredSize() {
        return inferredSize;
    }

    public void setInferredSize(long inferredSize) {
        this.inferredSize = inferredSize;
    }

    public boolean isThirdParty() {
        return isThirdParty;
    }

    public void setThirdParty(boolean thirdParty) {
        isThirdParty = thirdParty;
    }
}
