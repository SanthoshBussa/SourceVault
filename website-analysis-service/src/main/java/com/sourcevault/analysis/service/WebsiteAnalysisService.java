package com.sourcevault.analysis.service;

import com.sourcevault.analysis.parser.ResourceDiscoveryParser;
import com.sourcevault.common.dto.AnalysisRequest;
import com.sourcevault.common.dto.AnalysisResponse;
import com.sourcevault.common.dto.DiscoveredResourceDto;
import com.sourcevault.common.model.CollectionMode;
import com.sourcevault.common.model.ResourceType;
import com.sourcevault.common.security.SsrfValidator;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebsiteAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(WebsiteAnalysisService.class);

    private final ResourceDiscoveryParser parser;
    private final SsrfValidator ssrfValidator;
    private final Map<String, AnalysisResponse> cache = new ConcurrentHashMap<>();

    @Value("${sourcevault.analysis.connection-timeout-ms:10000}")
    private int connectionTimeoutMs;

    @Value("${sourcevault.analysis.user-agent:SourceVault-Collector/1.0}")
    private String userAgent;

    @Value("${sourcevault.analysis.max-html-size-bytes:10485760}")
    private int maxHtmlSizeBytes;

    public WebsiteAnalysisService(ResourceDiscoveryParser parser) {
        this.parser = parser;
        this.ssrfValidator = new SsrfValidator();
    }

    public AnalysisResponse analyzeWebsite(AnalysisRequest request) {
        String rawUrl = request.getWebsiteUrl();
        URI validatedUri = ssrfValidator.validateAndNormalize(rawUrl);
        String normalizedUrl = validatedUri.toString();

        AnalysisResponse response = new AnalysisResponse();
        String analysisId = "analysis_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        response.setAnalysisId(analysisId);
        response.setNormalizedUrl(normalizedUrl);

        String host = validatedUri.getHost();
        String origin = (validatedUri.getScheme() != null ? validatedUri.getScheme() : "https") + "://" + host +
                (validatedUri.getPort() != -1 ? ":" + validatedUri.getPort() : "");
        response.setWebsiteOrigin(origin);

        try {
            Connection connection = Jsoup.connect(normalizedUrl)
                    .userAgent(userAgent)
                    .timeout(connectionTimeoutMs)
                    .maxBodySize(maxHtmlSizeBytes)
                    .followRedirects(false)
                    .ignoreHttpErrors(true);

            Connection.Response res = connection.execute();
            int statusCode = res.statusCode();
            response.setHttpStatus(statusCode);

            // Handle Safe Redirects
            if (statusCode >= 300 && statusCode < 400) {
                String location = res.header("Location");
                if (location != null && !location.isEmpty()) {
                    URI redirectUri = validatedUri.resolve(location).normalize();
                    ssrfValidator.validateHost(redirectUri.getHost());
                    normalizedUrl = redirectUri.toString();
                    response.setNormalizedUrl(normalizedUrl);

                    res = Jsoup.connect(normalizedUrl)
                            .userAgent(userAgent)
                            .timeout(connectionTimeoutMs)
                            .maxBodySize(maxHtmlSizeBytes)
                            .followRedirects(true)
                            .ignoreHttpErrors(true)
                            .execute();
                    statusCode = res.statusCode();
                    response.setHttpStatus(statusCode);
                }
            }

            if (statusCode >= 200 && statusCode < 400) {
                response.setPageAccessible(true);
                Document doc = res.parse();
                response.setTitle(doc.title().isEmpty() ? host : doc.title());

                // Find favicon
                String favicon = null;
                org.jsoup.nodes.Element favEl = doc.selectFirst("link[rel~=(?i)^(shortcut|icon)$]");
                if (favEl != null) {
                    favicon = favEl.attr("abs:href");
                }
                if (favicon == null || favicon.isEmpty()) {
                    favicon = origin + "/favicon.ico";
                }
                response.setFaviconUrl(favicon);

                // Discover resources
                List<DiscoveredResourceDto> discovered = parser.parseResources(doc, URI.create(normalizedUrl));
                response.setDiscoveredResources(discovered);
                response.setTotalDiscovered(discovered.size());

                // Calculate breakdowns and source maps
                Map<String, Integer> typeBreakdown = new HashMap<>();
                Map<String, Integer> originBreakdown = new HashMap<>();
                int sourceMapCount = 0;
                long totalSizeEstimate = 0;

                for (DiscoveredResourceDto r : discovered) {
                    String typeName = r.getResourceType().name();
                    typeBreakdown.put(typeName, typeBreakdown.getOrDefault(typeName, 0) + 1);

                    String resourceOrigin = r.getOrigin();
                    originBreakdown.put(resourceOrigin, originBreakdown.getOrDefault(resourceOrigin, 0) + 1);

                    // Infer basic sizes
                    long size = switch (r.getResourceType()) {
                        case HTML -> 45_000L;
                        case SCRIPT -> 120_000L;
                        case STYLESHEET -> 35_000L;
                        case IMAGE -> 80_000L;
                        case FONT -> 90_000L;
                        case SOURCE_MAP -> 250_000L;
                        default -> 20_000L;
                    };
                    r.setInferredSize(size);
                    totalSizeEstimate += size;

                    // Check for potential source maps
                    if (r.getResourceType() == ResourceType.SCRIPT || r.getResourceType() == ResourceType.STYLESHEET) {
                        String potentialMap = r.getUrl().split("[?#]")[0] + ".map";
                        r.setSourceMapUrl(potentialMap);
                        sourceMapCount++;
                    }
                }

                response.setTypeBreakdown(typeBreakdown);
                response.setOriginBreakdown(originBreakdown);
                response.setSourceMapCount(sourceMapCount);
                response.setEstimatedSizeBytes(totalSizeEstimate);

                // Set warnings
                if (Boolean.TRUE.equals(request.getPreferBrowser())) {
                    response.setRecommendedMode(CollectionMode.BROWSER_INTEGRATED);
                } else {
                    response.setRecommendedMode(CollectionMode.URL_ONLY);
                    response.getWarnings().add("URL-only mode scans publicly accessible assets. Files requiring an active login session or private authentication are not available without the Chrome extension.");
                }

                if (discovered.stream().anyMatch(DiscoveredResourceDto::isThirdParty)) {
                    response.getWarnings().add("Cross-origin resources (e.g. CDNs, fonts, analytics) were discovered. Third-party collection is optional during download.");
                }

            } else {
                response.setPageAccessible(false);
                response.getWarnings().add("Website returned non-success HTTP status " + statusCode + ". Accessible resources may be limited.");
            }

        } catch (SecurityException se) {
            log.warn("Security violation during analysis: {}", se.getMessage());
            response.setPageAccessible(false);
            response.getWarnings().add("Security Error: " + se.getMessage());
        } catch (Exception e) {
            log.error("Failed to analyze website {}: {}", normalizedUrl, e.getMessage());
            response.setPageAccessible(false);
            response.getWarnings().add("Could not connect to website: " + e.getMessage());
        }

        cache.put(analysisId, response);
        return response;
    }

    public Optional<AnalysisResponse> getAnalysis(String analysisId) {
        return Optional.ofNullable(cache.get(analysisId));
    }
}
