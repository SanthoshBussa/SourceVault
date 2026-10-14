package com.sourcevault.collection.service;

import com.sourcevault.collection.client.SafeHttpClient;
import com.sourcevault.common.dto.BrowserResourceDto;
import com.sourcevault.common.dto.CreateJobRequest;
import com.sourcevault.common.dto.DiscoveredResourceDto;
import com.sourcevault.common.dto.JobProgressDto;
import com.sourcevault.common.dto.ResourceDto;
import com.sourcevault.common.model.CollectionMode;
import com.sourcevault.common.model.JobStatus;
import com.sourcevault.common.model.ResourceStatus;
import com.sourcevault.common.model.ResourceType;
import com.sourcevault.common.model.RetrievalMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ResourceCollectionService {

    private static final Logger log = LoggerFactory.getLogger(ResourceCollectionService.class);
    private static final Pattern SOURCE_MAP_COMMENT_PATTERN = Pattern.compile("//[#@]\\s*sourceMappingURL=([\\S]+)|/\\*[#@]\\s*sourceMappingURL=([\\S]+?)\\s*\\*/");

    private final SafeHttpClient httpClient;
    private final ResourceStorageService storageService;
    private final ExecutorService executorService;

    private final Map<String, CollectionJobRuntime> activeJobs = new ConcurrentHashMap<>();

    public ResourceCollectionService(SafeHttpClient httpClient,
                                     ResourceStorageService storageService,
                                     @Value("${sourcevault.collection.max-concurrent-downloads:6}") int concurrency) {
        this.httpClient = httpClient;
        this.storageService = storageService;
        this.executorService = Executors.newFixedThreadPool(concurrency);
    }

    public static class CollectionJobRuntime {
        private final String jobId;
        private final String websiteUrl;
        private final String websiteOrigin;
        private final CreateJobRequest options;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final AtomicInteger downloadedCount = new AtomicInteger(0);
        private final AtomicInteger failedCount = new AtomicInteger(0);
        private final AtomicInteger skippedCount = new AtomicInteger(0);
        private final AtomicInteger reconstructedCount = new AtomicInteger(0);
        private final AtomicLong totalBytes = new AtomicLong(0);
        private volatile String currentResourceUrl = "";
        private volatile JobStatus status = JobStatus.COLLECTING;
        private final List<ResourceDto> resources = new CopyOnWriteArrayList<>();
        private final List<String> eventLog = new CopyOnWriteArrayList<>();
        private final Instant startedAt = Instant.now();

        public CollectionJobRuntime(String jobId, String websiteUrl, String websiteOrigin, CreateJobRequest options) {
            this.jobId = jobId;
            this.websiteUrl = websiteUrl;
            this.websiteOrigin = websiteOrigin;
            this.options = options;
        }

        public void addEvent(String event) {
            if (eventLog.size() > 50) {
                eventLog.remove(0);
            }
            eventLog.add(Instant.now() + " - " + event);
        }

        public String getJobId() { return jobId; }
        public String getWebsiteUrl() { return websiteUrl; }
        public String getWebsiteOrigin() { return websiteOrigin; }
        public CreateJobRequest getOptions() { return options; }
        public AtomicBoolean getCancelled() { return cancelled; }
        public AtomicInteger getDownloadedCount() { return downloadedCount; }
        public AtomicInteger getFailedCount() { return failedCount; }
        public AtomicInteger getSkippedCount() { return skippedCount; }
        public AtomicInteger getReconstructedCount() { return reconstructedCount; }
        public AtomicLong getTotalBytes() { return totalBytes; }
        public String getCurrentResourceUrl() { return currentResourceUrl; }
        public void setCurrentResourceUrl(String currentResourceUrl) { this.currentResourceUrl = currentResourceUrl; }
        public JobStatus getStatus() { return status; }
        public void setStatus(JobStatus status) { this.status = status; }
        public List<ResourceDto> getResources() { return resources; }
        public List<String> getEventLog() { return eventLog; }
        public Instant getStartedAt() { return startedAt; }
    }

    public void startCollection(String jobId,
                                String websiteUrl,
                                String websiteOrigin,
                                CreateJobRequest options,
                                List<DiscoveredResourceDto> discoveredResources,
                                List<BrowserResourceDto> browserResources) {
        CollectionJobRuntime runtime = new CollectionJobRuntime(jobId, websiteUrl, websiteOrigin, options);
        activeJobs.put(jobId, runtime);

        CompletableFuture.runAsync(() -> executeCollection(runtime, discoveredResources, browserResources), executorService);
    }

    private void executeCollection(CollectionJobRuntime runtime,
                                   List<DiscoveredResourceDto> discovered,
                                   List<BrowserResourceDto> browserResources) {
        String jobId = runtime.getJobId();
        runtime.addEvent("Starting collection for " + runtime.getWebsiteUrl());

        Map<String, BrowserResourceDto> browserMap = new HashMap<>();
        if (browserResources != null) {
            for (BrowserResourceDto br : browserResources) {
                if (br.getUrl() != null) {
                    browserMap.put(br.getUrl(), br);
                }
            }
        }

        Set<String> processedUrls = Collections.newSetFromMap(new ConcurrentHashMap<>());
        List<String> urlsToProcess = new ArrayList<>();

        if (discovered != null) {
            for (DiscoveredResourceDto d : discovered) {
                if (d.getUrl() != null) {
                    urlsToProcess.add(d.getUrl());
                }
            }
        }

        // Also add any browser captured URLs not in discovered
        for (String bUrl : browserMap.keySet()) {
            if (!urlsToProcess.contains(bUrl)) {
                urlsToProcess.add(bUrl);
            }
        }

        int maxCount = runtime.getOptions().getMaxResourceCount();
        long maxBytes = runtime.getOptions().getMaxTotalBytes();

        for (String url : urlsToProcess) {
            if (runtime.getCancelled().get()) {
                runtime.setStatus(JobStatus.CANCELLED);
                runtime.addEvent("Collection cancelled by user.");
                break;
            }

            if (processedUrls.size() >= maxCount) {
                runtime.addEvent("Max resource count limit (" + maxCount + ") reached.");
                break;
            }

            if (runtime.getTotalBytes().get() >= maxBytes) {
                runtime.addEvent("Max total bytes limit (" + maxBytes + ") reached.");
                break;
            }

            processedUrls.add(url);
            runtime.setCurrentResourceUrl(url);

            ResourceDto resource = processResource(runtime, url, browserMap.get(url));
            runtime.getResources().add(resource);

            // If script/css and source maps are enabled, check for source map
            if (runtime.getOptions().isReconstructSourceMaps() &&
                    (resource.getResourceType() == ResourceType.SCRIPT || resource.getResourceType() == ResourceType.STYLESHEET) &&
                    resource.getStatus() == ResourceStatus.DOWNLOADED) {
                checkAndQueueSourceMap(runtime, resource, processedUrls);
            }
        }

        if (runtime.getStatus() != JobStatus.CANCELLED) {
            if (runtime.getFailedCount().get() > 0 && runtime.getDownloadedCount().get() > 0) {
                runtime.setStatus(JobStatus.COMPLETED_WITH_WARNINGS);
            } else if (runtime.getDownloadedCount().get() == 0 && runtime.getFailedCount().get() > 0) {
                runtime.setStatus(JobStatus.FAILED);
            } else {
                runtime.setStatus(JobStatus.COMPLETED);
            }
            runtime.addEvent("Collection finished with status: " + runtime.getStatus());
        }
    }

    private ResourceDto processResource(CollectionJobRuntime runtime, String url, BrowserResourceDto browserResource) {
        ResourceDto dto = new ResourceDto();
        dto.setId(UUID.randomUUID().toString());
        dto.setJobId(runtime.getJobId());
        dto.setOriginalUrl(url);
        dto.setCreatedAt(Instant.now());

        URI uri;
        try {
            uri = URI.create(url).normalize();
            dto.setResourceOrigin(getOrigin(uri));
            dto.setResourcePath(uri.getPath().isEmpty() ? "/" : uri.getPath());
            dto.setFileName(extractFileName(uri));
        } catch (Exception e) {
            dto.setStatus(ResourceStatus.FAILED);
            dto.setFailureReason("Malformed URL: " + e.getMessage());
            runtime.getFailedCount().incrementAndGet();
            return dto;
        }

        // Check origin filtering
        boolean isThirdParty = !dto.getResourceOrigin().equalsIgnoreCase(runtime.getWebsiteOrigin());
        if (isThirdParty && !runtime.getOptions().isIncludeThirdParty()) {
            dto.setStatus(ResourceStatus.SKIPPED);
            dto.setFailureReason("Third-party resource skipped per job configuration");
            runtime.getSkippedCount().incrementAndGet();
            return dto;
        }

        // 1. Browser captured payload reuse if available
        if (browserResource != null && browserResource.getBody() != null && !browserResource.getBody().isEmpty()) {
            try {
                byte[] data;
                if (browserResource.isBase64()) {
                    data = Base64.getDecoder().decode(browserResource.getBody());
                } else {
                    data = browserResource.getBody().getBytes(StandardCharsets.UTF_8);
                }

                dto.setRetrievalMethod(RetrievalMethod.BROWSER_CAPTURED);
                dto.setContentType(browserResource.getContentType() != null ? browserResource.getContentType() : "application/octet-stream");
                dto.setHttpStatus(browserResource.getHttpStatus());
                dto.setFileSize((long) data.length);
                dto.setChecksum(computeSha256(data));
                dto.setResourceType(browserResource.getResourceType() != null ? browserResource.getResourceType() : classifyType(url, dto.getContentType()));

                if (browserResource.getHttpStatus() >= 200 && browserResource.getHttpStatus() < 400) {
                    dto.setStatus(ResourceStatus.DOWNLOADED);
                    storageService.storeResource(runtime.getJobId(), dto.getChecksum(), data);
                    runtime.getDownloadedCount().incrementAndGet();
                    runtime.getTotalBytes().addAndGet(data.length);
                    runtime.addEvent("Retrieved from browser: " + dto.getFileName() + " (" + data.length + " bytes)");
                } else {
                    dto.setStatus(ResourceStatus.FAILED);
                    dto.setFailureReason("Browser captured HTTP " + browserResource.getHttpStatus());
                    runtime.getFailedCount().incrementAndGet();
                }
                return dto;
            } catch (Exception e) {
                log.warn("Failed decoding browser resource {}: {}", url, e.getMessage());
            }
        }

        // 2. HTTP download
        try {
            dto.setRetrievalMethod(RetrievalMethod.HTTP_PUBLIC);
            SafeHttpClient.DownloadResult result = httpClient.downloadResource(url);
            dto.setHttpStatus(result.getStatusCode());
            dto.setContentType(result.getContentType());
            dto.setFileSize(result.getSize());
            dto.setChecksum(result.getChecksumSha256());
            dto.setResourceType(classifyType(url, result.getContentType()));

            if (result.getStatusCode() >= 200 && result.getStatusCode() < 400) {
                dto.setStatus(ResourceStatus.DOWNLOADED);
                storageService.storeResource(runtime.getJobId(), dto.getChecksum(), result.getData());
                runtime.getDownloadedCount().incrementAndGet();
                runtime.getTotalBytes().addAndGet(result.getSize());
                runtime.addEvent("Downloaded: " + dto.getFileName() + " (" + result.getSize() + " bytes)");
            } else {
                dto.setStatus(ResourceStatus.FAILED);
                dto.setFailureReason("HTTP error " + result.getStatusCode());
                runtime.getFailedCount().incrementAndGet();
                runtime.addEvent("Failed HTTP " + result.getStatusCode() + ": " + url);
            }
        } catch (Exception e) {
            dto.setStatus(ResourceStatus.FAILED);
            dto.setFailureReason(e.getMessage());
            runtime.getFailedCount().incrementAndGet();
            runtime.addEvent("Download failed: " + url + " - " + e.getMessage());
        }

        return dto;
    }

    private void checkAndQueueSourceMap(CollectionJobRuntime runtime, ResourceDto resource, Set<String> processedUrls) {
        try {
            byte[] bytes = storageService.readResource(runtime.getJobId(), resource.getChecksum());
            if (bytes == null || bytes.length == 0) return;

            String text = new String(bytes, StandardCharsets.UTF_8);
            Matcher matcher = SOURCE_MAP_COMMENT_PATTERN.matcher(text);
            String mapRef = null;
            while (matcher.find()) {
                mapRef = matcher.group(1) != null ? matcher.group(1).trim() : matcher.group(2).trim();
            }

            if (mapRef == null) {
                // Try .map convention
                mapRef = resource.getFileName() + ".map";
            }

            String fullMapUrl;
            if (mapRef.startsWith("data:")) {
                // Embedded source map! Store it directly
                byte[] mapData = parseDataUri(mapRef);
                if (mapData != null) {
                    storeEmbeddedMap(runtime, resource, mapData);
                }
                return;
            } else {
                URI baseUri = URI.create(resource.getOriginalUrl());
                fullMapUrl = baseUri.resolve(mapRef).normalize().toString();
            }

            if (!processedUrls.contains(fullMapUrl)) {
                processedUrls.add(fullMapUrl);
                ResourceDto mapResource = processResource(runtime, fullMapUrl, null);
                mapResource.setResourceType(ResourceType.SOURCE_MAP);
                runtime.getResources().add(mapResource);
            }
        } catch (Exception e) {
            log.debug("No source map found for {}: {}", resource.getOriginalUrl(), e.getMessage());
        }
    }

    private void storeEmbeddedMap(CollectionJobRuntime runtime, ResourceDto parent, byte[] mapData) throws Exception {
        String mapChecksum = computeSha256(mapData);
        storageService.storeResource(runtime.getJobId(), mapChecksum, mapData);

        ResourceDto mapDto = new ResourceDto();
        mapDto.setId(UUID.randomUUID().toString());
        mapDto.setJobId(runtime.getJobId());
        mapDto.setOriginalUrl(parent.getOriginalUrl() + ".map");
        mapDto.setResourceOrigin(parent.getResourceOrigin());
        mapDto.setResourcePath(parent.getResourcePath() + ".map");
        mapDto.setFileName(parent.getFileName() + ".map");
        mapDto.setResourceType(ResourceType.SOURCE_MAP);
        mapDto.setContentType("application/json");
        mapDto.setHttpStatus(200);
        mapDto.setFileSize((long) mapData.length);
        mapDto.setChecksum(mapChecksum);
        mapDto.setRetrievalMethod(RetrievalMethod.HTTP_PUBLIC);
        mapDto.setStatus(ResourceStatus.DOWNLOADED);
        mapDto.setCreatedAt(Instant.now());

        runtime.getResources().add(mapDto);
        runtime.getDownloadedCount().incrementAndGet();
        runtime.getTotalBytes().addAndGet(mapData.length);
        runtime.addEvent("Extracted inline source map for " + parent.getFileName());
    }

    private byte[] parseDataUri(String dataUri) {
        try {
            int commaIdx = dataUri.indexOf(',');
            if (commaIdx > 0) {
                String meta = dataUri.substring(0, commaIdx);
                String data = dataUri.substring(commaIdx + 1);
                if (meta.contains(";base64")) {
                    return Base64.getDecoder().decode(data);
                } else {
                    return java.net.URLDecoder.decode(data, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {
            log.warn("Failed parsing data URI source map: {}", e.getMessage());
        }
        return null;
    }

    public boolean cancelJob(String jobId) {
        CollectionJobRuntime runtime = activeJobs.get(jobId);
        if (runtime != null) {
            runtime.getCancelled().set(true);
            runtime.setStatus(JobStatus.CANCELLED);
            runtime.addEvent("Cancellation requested.");
            return true;
        }
        return false;
    }

    public JobProgressDto getProgress(String jobId) {
        CollectionJobRuntime runtime = activeJobs.get(jobId);
        if (runtime == null) {
            return null;
        }

        JobProgressDto dto = new JobProgressDto();
        dto.setJobId(jobId);
        dto.setStatus(runtime.getStatus());
        dto.setTotalDownloaded(runtime.getDownloadedCount().get());
        dto.setTotalFailed(runtime.getFailedCount().get());
        dto.setTotalSkipped(runtime.getSkippedCount().get());
        dto.setTotalReconstructed(runtime.getReconstructedCount().get());
        dto.setTotalBytes(runtime.getTotalBytes().get());
        dto.setCurrentResourceUrl(runtime.getCurrentResourceUrl());
        dto.setRecentEvents(new ArrayList<>(runtime.getEventLog()));
        dto.setElapsedSeconds((Instant.now().toEpochMilli() - runtime.getStartedAt().toEpochMilli()) / 1000);

        int total = runtime.getDownloadedCount().get() + runtime.getFailedCount().get() + runtime.getSkippedCount().get();
        int expected = runtime.getResources().size();
        dto.setTotalDiscovered(expected);
        dto.setPercentage(expected > 0 ? Math.min(100.0, (double) total * 100.0 / expected) : 0.0);

        return dto;
    }

    public List<ResourceDto> getResources(String jobId) {
        CollectionJobRuntime runtime = activeJobs.get(jobId);
        return runtime != null ? runtime.getResources() : Collections.emptyList();
    }

    public Optional<CollectionJobRuntime> getJobRuntime(String jobId) {
        return Optional.ofNullable(activeJobs.get(jobId));
    }

    private ResourceType classifyType(String url, String contentType) {
        String path = url.split("[?#]")[0].toLowerCase(Locale.ROOT);
        if (path.endsWith(".js") || path.endsWith(".mjs")) return ResourceType.SCRIPT;
        if (path.endsWith(".css")) return ResourceType.STYLESHEET;
        if (path.endsWith(".map")) return ResourceType.SOURCE_MAP;
        if (path.endsWith(".json")) return ResourceType.JSON;
        if (path.endsWith(".webmanifest") || path.endsWith("manifest.json")) return ResourceType.MANIFEST;
        if (path.endsWith(".woff2") || path.endsWith(".woff") || path.endsWith(".ttf")) return ResourceType.FONT;
        if (path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".svg") || path.endsWith(".webp") || path.endsWith(".ico")) return ResourceType.IMAGE;
        if (contentType != null) {
            if (contentType.contains("javascript")) return ResourceType.SCRIPT;
            if (contentType.contains("css")) return ResourceType.STYLESHEET;
            if (contentType.contains("json")) return ResourceType.JSON;
            if (contentType.contains("image/")) return ResourceType.IMAGE;
            if (contentType.contains("font") || contentType.contains("woff")) return ResourceType.FONT;
            if (contentType.contains("html")) return ResourceType.HTML;
        }
        return ResourceType.OTHER;
    }

    private String extractFileName(URI uri) {
        String path = uri.getPath();
        if (path == null || path.isEmpty() || path.equals("/")) {
            return "index.html";
        }
        int lastSlash = path.lastIndexOf('/');
        String name = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
        return name.isEmpty() ? "index.html" : name;
    }

    private String getOrigin(URI uri) {
        String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase(Locale.ROOT) : "https";
        String host = uri.getHost() != null ? uri.getHost().toLowerCase(Locale.ROOT) : "";
        int port = uri.getPort();
        if (port == -1 || (port == 80 && "http".equals(scheme)) || (port == 443 && "https".equals(scheme))) {
            return scheme + "://" + host;
        }
        return scheme + "://" + host + ":" + port;
    }

    private String computeSha256(byte[] data) throws Exception {
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        return HexFormat.of().formatHex(hash);
    }
}
