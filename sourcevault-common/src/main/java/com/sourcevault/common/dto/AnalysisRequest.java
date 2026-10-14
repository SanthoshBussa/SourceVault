package com.sourcevault.common.dto;

import jakarta.validation.constraints.NotBlank;

public class AnalysisRequest {

    @NotBlank(message = "Website URL cannot be blank")
    private String websiteUrl;

    private Boolean preferBrowser = false;

    public AnalysisRequest() {}

    public AnalysisRequest(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public AnalysisRequest(String websiteUrl, Boolean preferBrowser) {
        this.websiteUrl = websiteUrl;
        this.preferBrowser = preferBrowser;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public Boolean getPreferBrowser() {
        return preferBrowser;
    }

    public void setPreferBrowser(Boolean preferBrowser) {
        this.preferBrowser = preferBrowser;
    }
}
