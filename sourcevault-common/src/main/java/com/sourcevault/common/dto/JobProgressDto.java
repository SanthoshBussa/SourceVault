package com.sourcevault.common.dto;

import com.sourcevault.common.model.JobStatus;

import java.util.ArrayList;
import java.util.List;

public class JobProgressDto {
    private String jobId;
    private JobStatus status;
    private int totalDiscovered;
    private int totalDownloaded;
    private int totalFailed;
    private int totalSkipped;
    private int totalReconstructed;
    private long totalBytes;
    private double percentage;
    private String currentResourceUrl;
    private long elapsedSeconds;
    private List<String> recentEvents = new ArrayList<>();

    public JobProgressDto() {}

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
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

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public String getCurrentResourceUrl() {
        return currentResourceUrl;
    }

    public void setCurrentResourceUrl(String currentResourceUrl) {
        this.currentResourceUrl = currentResourceUrl;
    }

    public long getElapsedSeconds() {
        return elapsedSeconds;
    }

    public void setElapsedSeconds(long elapsedSeconds) {
        this.elapsedSeconds = elapsedSeconds;
    }

    public List<String> getRecentEvents() {
        return recentEvents;
    }

    public void setRecentEvents(List<String> recentEvents) {
        this.recentEvents = recentEvents;
    }
}
