package com.sourcevault.archive.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sourcevault.archive.sourcemap.SourceMapReconstructor;
import com.sourcevault.archive.validator.ArchiveValidator;
import com.sourcevault.archive.zip.ZipPackageBuilder;
import com.sourcevault.common.dto.ArchiveManifestItem;
import com.sourcevault.common.dto.ResourceDto;
import com.sourcevault.common.model.ResourceStatus;
import com.sourcevault.common.model.ResourceType;
import com.sourcevault.common.model.RetrievalMethod;
import com.sourcevault.common.security.PathSanitizer;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Service
public class ArchiveService {

    private static final Logger log = LoggerFactory.getLogger(ArchiveService.class);
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final SourceMapReconstructor sourceMapReconstructor;
    private final ZipPackageBuilder zipBuilder;
    private final ArchiveValidator archiveValidator;

    private final Path outputDir;
    private final Path tempStagingDir;

    private final Map<String, ArchiveMetadata> jobArchives = new ConcurrentHashMap<>();

    public ArchiveService(SourceMapReconstructor sourceMapReconstructor,
                          ZipPackageBuilder zipBuilder,
                          ArchiveValidator archiveValidator,
                          @Value("${sourcevault.archive.output-dir:./output/archives}") String outputDirStr,
                          @Value("${sourcevault.archive.temp-dir:./data/temp-staging}") String tempDirStr) {
        this.sourceMapReconstructor = sourceMapReconstructor;
        this.zipBuilder = zipBuilder;
        this.archiveValidator = archiveValidator;
        this.outputDir = Paths.get(outputDirStr).toAbsolutePath().normalize();
        this.tempStagingDir = Paths.get(tempDirStr).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.outputDir);
            Files.createDirectories(this.tempStagingDir);
        } catch (IOException e) {
            log.error("Failed creating archive directories", e);
        }
    }

    public static class ArchiveMetadata {
        private final String jobId;
        private final String archiveFileName;
        private final Path archivePath;
        private final long archiveSize;
        private final String checksumSha256;
        private final String validationStatus;
        private final int totalFiles;
        private final int reconstructedCount;
        private final String manifestCsv;
        private final String failedReport;

        public ArchiveMetadata(String jobId, String archiveFileName, Path archivePath, long archiveSize,
                               String checksumSha256, String validationStatus, int totalFiles,
                               int reconstructedCount, String manifestCsv, String failedReport) {
            this.jobId = jobId;
            this.archiveFileName = archiveFileName;
            this.archivePath = archivePath;
            this.archiveSize = archiveSize;
            this.checksumSha256 = checksumSha256;
            this.validationStatus = validationStatus;
            this.totalFiles = totalFiles;
            this.reconstructedCount = reconstructedCount;
            this.manifestCsv = manifestCsv;
            this.failedReport = failedReport;
        }

        public String getJobId() { return jobId; }
        public String getArchiveFileName() { return archiveFileName; }
        public Path getArchivePath() { return archivePath; }
        public long getArchiveSize() { return archiveSize; }
        public String getChecksumSha256() { return checksumSha256; }
        public String getValidationStatus() { return validationStatus; }
        public int getTotalFiles() { return totalFiles; }
        public int getReconstructedCount() { return reconstructedCount; }
        public String getManifestCsv() { return manifestCsv; }
        public String getFailedReport() { return failedReport; }
    }

    /**
     * Builds the complete archive for a job.
     */
    public ArchiveMetadata createArchive(String jobId,
                                         String websiteUrl,
                                         List<ResourceDto> resources,
                                         Function<String, byte[]> dataLoader) throws Exception {

        String safeHost = sanitizeHost(websiteUrl);
        String rootFolderName = safeHost + "_source_backup";
        Path stagingDir = tempStagingDir.resolve(jobId).resolve(rootFolderName);
        Files.createDirectories(stagingDir);

        List<ArchiveManifestItem> manifestItems = new ArrayList<>();
        List<String> failedReports = new ArrayList<>();
        int reconstructedCount = 0;

        try {
            // 1. Process downloaded resources
            for (ResourceDto r : resources) {
                if (r.getStatus() == ResourceStatus.DOWNLOADED) {
                    byte[] data = dataLoader.apply(r.getChecksum());
                    if (data == null) {
                        failedReports.add("Missing data for " + r.getOriginalUrl());
                        continue;
                    }

                    // Compute clean relative path: host/path
                    String relPath = buildRelativePath(r);
                    Path targetFile = PathSanitizer.resolveSafe(stagingDir, relPath);
                    if (targetFile.getParent() != null) {
                        Files.createDirectories(targetFile.getParent());
                    }

                    Files.write(targetFile, data);

                    ArchiveManifestItem item = new ArchiveManifestItem(
                            r.getOriginalUrl(),
                            relPath,
                            r.getResourceType(),
                            r.getContentType(),
                            r.getHttpStatus() != null ? r.getHttpStatus() : 200,
                            data.length,
                            r.getChecksum(),
                            r.getRetrievalMethod() != null ? r.getRetrievalMethod() : RetrievalMethod.HTTP_PUBLIC,
                            false
                    );
                    manifestItems.add(item);

                    // If source map, attempt source reconstruction
                    if (r.getResourceType() == ResourceType.SOURCE_MAP || r.getOriginalUrl().endsWith(".map")) {
                        SourceMapReconstructor.ReconstructionResult rec =
                                sourceMapReconstructor.reconstruct(data, r.getOriginalUrl(), stagingDir);
                        if (rec.getReconstructedCount() > 0) {
                            reconstructedCount += rec.getReconstructedCount();
                            manifestItems.addAll(rec.getReconstructedItems());
                        }
                    }

                } else {
                    failedReports.add(r.getStatus() + ": " + r.getOriginalUrl() + " - " +
                            (r.getFailureReason() != null ? r.getFailureReason() : ""));
                }
            }

            // 2. Generate manifest.csv
            String manifestCsv = buildManifestCsv(manifestItems);
            Path manifestFile = stagingDir.resolve("manifest.csv");
            Files.writeString(manifestFile, manifestCsv, StandardCharsets.UTF_8);

            // 3. Generate failed_resources.txt
            String failedReportStr = String.join("\n", failedReports);
            Path failedFile = stagingDir.resolve("failed_resources.txt");
            Files.writeString(failedFile, failedReportStr, StandardCharsets.UTF_8);

            // 4. Generate README.md
            String readme = buildReadme(websiteUrl, manifestItems.size(), reconstructedCount, failedReports.size());
            Path readmeFile = stagingDir.resolve("README.md");
            Files.writeString(readmeFile, readme, StandardCharsets.UTF_8);

            // 5. Generate summary.json
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("websiteUrl", websiteUrl);
            summary.put("generatedAt", Instant.now().toString());
            summary.put("totalFiles", manifestItems.size());
            summary.put("reconstructedSourceFiles", reconstructedCount);
            summary.put("failedOrSkippedCount", failedReports.size());
            Path summaryFile = stagingDir.resolve("summary.json");
            Files.writeString(summaryFile, objectMapper.writeValueAsString(summary), StandardCharsets.UTF_8);

            // 6. Create ZIP package
            String zipFileName = rootFolderName + "_" + jobId.substring(0, Math.min(8, jobId.length())) + ".zip";
            Path targetZip = outputDir.resolve(zipFileName);

            ZipPackageBuilder.ZipResult zipResult = zipBuilder.createZip(stagingDir, targetZip, rootFolderName);

            // 7. Validate Archive
            ArchiveValidator.ValidationReport validation = archiveValidator.validateArchive(targetZip, manifestItems, rootFolderName);

            ArchiveMetadata metadata = new ArchiveMetadata(
                    jobId,
                    zipFileName,
                    targetZip,
                    zipResult.getSizeBytes(),
                    zipResult.getSha256Checksum(),
                    validation.getStatus(),
                    manifestItems.size(),
                    reconstructedCount,
                    manifestCsv,
                    failedReportStr
            );

            jobArchives.put(jobId, metadata);
            return metadata;

        } finally {
            // Clean up staging directory
            try {
                FileUtils.deleteDirectory(tempStagingDir.resolve(jobId).toFile());
            } catch (Exception e) {
                log.warn("Could not delete staging directory for {}: {}", jobId, e.getMessage());
            }
        }
    }

    public Optional<ArchiveMetadata> getArchiveMetadata(String jobId) {
        return Optional.ofNullable(jobArchives.get(jobId));
    }

    private String buildRelativePath(ResourceDto resource) {
        String host = "unknown";
        String path = "index.html";

        try {
            URI uri = URI.create(resource.getOriginalUrl());
            if (uri.getHost() != null) {
                host = uri.getHost().replaceAll("[\\\\/:*?\"<>|]", "_");
            }
            if (uri.getPath() != null && !uri.getPath().isEmpty() && !uri.getPath().equals("/")) {
                path = uri.getPath();
                if (path.startsWith("/")) path = path.substring(1);
            }
        } catch (Exception e) {
            host = "resources";
        }

        if (path.isEmpty() || path.endsWith("/")) {
            path += "index.html";
        }

        return host + "/" + path;
    }

    private String buildManifestCsv(List<ArchiveManifestItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("Original URL,Local Path,Resource Type,Content Type,HTTP Status,File Size,SHA-256 Checksum,Retrieval Method,Is Reconstructed\n");
        for (ArchiveManifestItem item : items) {
            sb.append('"').append(escapeCsv(item.getOriginalUrl())).append("\",")
                    .append('"').append(escapeCsv(item.getLocalPath())).append("\",")
                    .append(item.getResourceType()).append(',')
                    .append('"').append(escapeCsv(item.getContentType())).append("\",")
                    .append(item.getHttpStatus()).append(',')
                    .append(item.getFileSize()).append(',')
                    .append(item.getChecksum()).append(',')
                    .append(item.getRetrievalMethod()).append(',')
                    .append(item.isReconstructed()).append('\n');
        }
        return sb.toString();
    }

    private String escapeCsv(String val) {
        return val != null ? val.replace("\"", "\"\"") : "";
    }

    private String buildReadme(String websiteUrl, int totalFiles, int reconstructed, int failed) {
        return "# SourceVault Backup Archive\n\n" +
                "- **Target Website:** " + websiteUrl + "\n" +
                "- **Backup Generated At:** " + Instant.now() + "\n" +
                "- **Total Assets Packaged:** " + totalFiles + "\n" +
                "- **Reconstructed Source Files:** " + reconstructed + "\n" +
                "- **Failed or Skipped Resources:** " + failed + "\n\n" +
                "## Structure\n" +
                "- Each host domain is kept in its own directory (e.g. `topbrains.com/`).\n" +
                "- Generated bundles (compiled JS/CSS) are located in their respective domain folders.\n" +
                "- Original source files recovered from source maps (via `sourcesContent`) are stored in `reconstructed_sources/`.\n" +
                "- `manifest.csv` contains SHA-256 checksums and provenance of every file.\n" +
                "- `failed_resources.txt` documents resources that could not be downloaded.\n\n" +
                "*Generated automatically by SourceVault.*";
    }

    private String sanitizeHost(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host != null) {
                return host.replaceAll("[^a-zA-Z0-9.-]", "_");
            }
        } catch (Exception ignored) {}
        return "website";
    }
}
