package com.sourcevault.common.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class BrowserSessionDto {
    private String sessionId;
    private String browserName;
    private String extensionVersion;
    private String activeTabUrl;
    private String activeTabTitle;
    private Instant connectedAt;
    private Instant lastPingAt;
    private boolean active;
    private List<BrowserResourceDto> pendingResources = new ArrayList<>();

    public BrowserSessionDto() {}

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getBrowserName() {
        return browserName;
    }

    public void setBrowserName(String browserName) {
        this.browserName = browserName;
    }

    public String getExtensionVersion() {
        return extensionVersion;
    }

    public void setExtensionVersion(String extensionVersion) {
        this.extensionVersion = extensionVersion;
    }

    public String getActiveTabUrl() {
        return activeTabUrl;
    }

    public void setActiveTabUrl(String activeTabUrl) {
        this.activeTabUrl = activeTabUrl;
    }

    public String getActiveTabTitle() {
        return activeTabTitle;
    }

    public void setActiveTabTitle(String activeTabTitle) {
        this.activeTabTitle = activeTabTitle;
    }

    public Instant getConnectedAt() {
        return connectedAt;
    }

    public void setConnectedAt(Instant connectedAt) {
        this.connectedAt = connectedAt;
    }

    public Instant getLastPingAt() {
        return lastPingAt;
    }

    public void setLastPingAt(Instant lastPingAt) {
        this.lastPingAt = lastPingAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<BrowserResourceDto> getPendingResources() {
        return pendingResources;
    }

    public void setPendingResources(List<BrowserResourceDto> pendingResources) {
        this.pendingResources = pendingResources;
    }
}
