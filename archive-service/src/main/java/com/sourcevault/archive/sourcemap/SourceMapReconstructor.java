package com.sourcevault.archive.sourcemap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sourcevault.common.dto.ArchiveManifestItem;
import com.sourcevault.common.model.ResourceType;
import com.sourcevault.common.model.RetrievalMethod;
import com.sourcevault.common.security.PathSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Component
public class SourceMapReconstructor {

    private static final Logger log = LoggerFactory.getLogger(SourceMapReconstructor.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static class ReconstructionResult {
        private final int reconstructedCount;
        private final List<ArchiveManifestItem> reconstructedItems = new ArrayList<>();

        public ReconstructionResult(int count, List<ArchiveManifestItem> items) {
            this.reconstructedCount = count;
            this.reconstructedItems.addAll(items);
        }

        public int getReconstructedCount() {
            return reconstructedCount;
        }

        public List<ArchiveManifestItem> getReconstructedItems() {
            return reconstructedItems;
        }
    }

    /**
     * Reconstructs original source files from a source map JSON if sourcesContent is present.
     */
    public ReconstructionResult reconstruct(byte[] sourceMapBytes, String originalMapUrl, Path stagingRoot) {
        List<ArchiveManifestItem> items = new ArrayList<>();
        if (sourceMapBytes == null || sourceMapBytes.length == 0) {
            return new ReconstructionResult(0, items);
        }

        try {
            JsonNode root = objectMapper.readTree(sourceMapBytes);
            JsonNode sourcesNode = root.get("sources");
            JsonNode sourcesContentNode = root.get("sourcesContent");

            if (sourcesNode == null || !sourcesNode.isArray()) {
                log.debug("No 'sources' array found in source map for {}", originalMapUrl);
                return new ReconstructionResult(0, items);
            }

            if (sourcesContentNode == null || !sourcesContentNode.isArray()) {
                log.info("Source map for {} contains sources but NO sourcesContent (cannot reconstruct without content)", originalMapUrl);
                return new ReconstructionResult(0, items);
            }

            Path reconstructedDir = stagingRoot.resolve("reconstructed_sources");
            Files.createDirectories(reconstructedDir);

            int count = 0;
            for (int i = 0; i < sourcesNode.size(); i++) {
                String sourcePath = sourcesNode.get(i).asText();
                if (i >= sourcesContentNode.size()) {
                    continue;
                }

                JsonNode contentNode = sourcesContentNode.get(i);
                if (contentNode == null || contentNode.isNull()) {
                    continue; // Skip sources without embedded content
                }

                String sourceContent = contentNode.asText();
                if (sourceContent == null || sourceContent.isEmpty()) {
                    continue;
                }

                // Clean virtual path like webpack:///src/App.tsx, ng://..., or relative ../
                String cleanPath = sanitizeSourcePath(sourcePath);
                Path targetFilePath = PathSanitizer.resolveSafe(reconstructedDir, cleanPath);

                if (targetFilePath.getParent() != null) {
                    Files.createDirectories(targetFilePath.getParent());
                }

                byte[] contentBytes = sourceContent.getBytes(StandardCharsets.UTF_8);
                Files.write(targetFilePath, contentBytes);

                String checksum = computeSha256(contentBytes);
                String relativeZipPath = "reconstructed_sources/" + reconstructedDir.relativize(targetFilePath).toString().replace('\\', '/');

                ArchiveManifestItem item = new ArchiveManifestItem(
                        originalMapUrl + "#" + sourcePath,
                        relativeZipPath,
                        classifyReconstructedType(cleanPath),
                        "text/plain",
                        200,
                        contentBytes.length,
                        checksum,
                        RetrievalMethod.SOURCE_MAP_RECONSTRUCTED,
                        true
                );
                items.add(item);
                count++;
            }

            return new ReconstructionResult(count, items);
        } catch (Exception e) {
            log.warn("Failed reconstructing source map from {}: {}", originalMapUrl, e.getMessage());
            return new ReconstructionResult(0, items);
        }
    }

    private String sanitizeSourcePath(String rawPath) {
        if (rawPath == null || rawPath.isEmpty()) {
            return "unnamed_source.js";
        }

        String path = rawPath.trim();
        // Remove virtual protocol prefixes
        path = path.replaceAll("^[a-zA-Z0-9_+-]+:///?", "");
        // Remove ~ and leading ./ or /
        path = path.replaceAll("^~/?", "");
        path = path.replace('\\', '/');
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        // Remove ../
        path = path.replace("../", "parent/");

        return path.isEmpty() ? "unnamed_source.js" : path;
    }

    private ResourceType classifyReconstructedType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".ts") || lower.endsWith(".tsx") || lower.endsWith(".js") || lower.endsWith(".jsx")) {
            return ResourceType.SCRIPT;
        } else if (lower.endsWith(".css") || lower.endsWith(".scss") || lower.endsWith(".sass") || lower.endsWith(".less")) {
            return ResourceType.STYLESHEET;
        } else if (lower.endsWith(".json")) {
            return ResourceType.JSON;
        } else if (lower.endsWith(".html")) {
            return ResourceType.HTML;
        }
        return ResourceType.OTHER;
    }

    private String computeSha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(data));
    }
}
