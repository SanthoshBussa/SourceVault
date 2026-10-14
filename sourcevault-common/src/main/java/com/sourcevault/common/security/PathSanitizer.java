package com.sourcevault.common.security;

import java.io.File;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Sanitizes and normalizes file paths to prevent directory traversal and illegal characters.
 */
public class PathSanitizer {

    private static final Pattern ILLEGAL_CHARS = Pattern.compile("[\\\\/:*?\"<>|]");
    private static final Pattern MULTI_SLASH = Pattern.compile("/{2,}");

    /**
     * Resolves and verifies that the child path remains strictly within target directory.
     */
    public static Path resolveSafe(Path baseDir, String relativePath) {
        if (baseDir == null) {
            throw new IllegalArgumentException("Base directory cannot be null");
        }
        if (relativePath == null || relativePath.trim().isEmpty()) {
            relativePath = "index.html";
        }

        // Clean slashes and remove query strings/fragments
        String cleaned = relativePath.split("[?#]")[0];
        cleaned = cleaned.replace('\\', '/');

        if (cleaned.contains("..")) {
            throw new SecurityException("Path traversal attempt detected: " + relativePath);
        }

        while (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }

        if (cleaned.isEmpty()) {
            cleaned = "index.html";
        }

        Path resolved = baseDir.resolve(cleaned).normalize();
        if (!resolved.startsWith(baseDir.normalize())) {
            throw new SecurityException("Path traversal attempt detected: " + relativePath);
        }

        return resolved;
    }

    /**
     * Sanitizes an individual file name or segment.
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "unnamed_resource";
        }

        String cleaned = filename.split("[?#]")[0];
        cleaned = ILLEGAL_CHARS.matcher(cleaned).replaceAll("_");
        cleaned = cleaned.replace("..", "_");

        if (cleaned.length() > 200) {
            String ext = "";
            int lastDot = cleaned.lastIndexOf('.');
            if (lastDot > 0 && lastDot > cleaned.length() - 10) {
                ext = cleaned.substring(lastDot);
            }
            cleaned = cleaned.substring(0, 190) + ext;
        }

        return cleaned;
    }
}
