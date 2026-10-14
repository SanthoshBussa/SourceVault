package com.sourcevault.archive.validator;

import com.sourcevault.common.dto.ArchiveManifestItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Component
public class ArchiveValidator {

    private static final Logger log = LoggerFactory.getLogger(ArchiveValidator.class);

    public static class ValidationReport {
        private final boolean valid;
        private final String status; // "VALID", "VALID_PARTIAL", "FAILED"
        private final int totalManifestEntries;
        private final int verifiedEntries;
        private final List<String> issues = new ArrayList<>();

        public ValidationReport(boolean valid, String status, int totalManifestEntries, int verifiedEntries) {
            this.valid = valid;
            this.status = status;
            this.totalManifestEntries = totalManifestEntries;
            this.verifiedEntries = verifiedEntries;
        }

        public void addIssue(String issue) {
            this.issues.add(issue);
        }

        public boolean isValid() { return valid; }
        public String getStatus() { return status; }
        public int getTotalManifestEntries() { return totalManifestEntries; }
        public int getVerifiedEntries() { return verifiedEntries; }
        public List<String> getIssues() { return issues; }
    }

    /**
     * Inspects and validates the integrity of the generated ZIP against manifest items.
     */
    public ValidationReport validateArchive(Path zipPath, List<ArchiveManifestItem> manifestItems, String rootFolderName) {
        if (!Files.exists(zipPath)) {
            ValidationReport report = new ValidationReport(false, "FAILED", manifestItems.size(), 0);
            report.addIssue("Archive ZIP file does not exist at " + zipPath);
            return report;
        }

        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            Map<String, ZipEntry> zipEntries = new HashMap<>();
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                zipEntries.put(entry.getName(), entry);
            }

            int verified = 0;
            List<String> issues = new ArrayList<>();

            // Verify readability and CRC checksum of entries
            byte[] buffer = new byte[8192];
            for (ArchiveManifestItem item : manifestItems) {
                String expectedZipPath = (rootFolderName != null && !rootFolderName.isEmpty())
                        ? rootFolderName + "/" + item.getLocalPath()
                        : item.getLocalPath();

                ZipEntry entry = zipEntries.get(expectedZipPath);
                if (entry == null) {
                    issues.add("Missing manifest entry in ZIP: " + expectedZipPath);
                    continue;
                }

                // Read bytes to verify CRC32 and integrity
                try (InputStream is = zipFile.getInputStream(entry)) {
                    long readCount = 0;
                    int r;
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    while ((r = is.read(buffer)) != -1) {
                        digest.update(buffer, 0, r);
                        readCount += r;
                    }

                    if (readCount != item.getFileSize()) {
                        issues.add("Size mismatch for " + expectedZipPath + ": expected " + item.getFileSize() + ", found " + readCount);
                    } else {
                        verified++;
                    }
                } catch (Exception e) {
                    issues.add("CRC/read error in entry " + expectedZipPath + ": " + e.getMessage());
                }
            }

            boolean allVerified = (verified == manifestItems.size() && issues.isEmpty());
            String status = allVerified ? "VALID" : (verified > 0 ? "VALID_PARTIAL" : "FAILED");

            ValidationReport report = new ValidationReport(allVerified || verified > 0, status, manifestItems.size(), verified);
            report.getIssues().addAll(issues);
            return report;

        } catch (Exception e) {
            log.error("Failed opening zip file {}: {}", zipPath, e.getMessage());
            ValidationReport report = new ValidationReport(false, "FAILED", manifestItems.size(), 0);
            report.addIssue("ZIP structure is corrupted or unreadable: " + e.getMessage());
            return report;
        }
    }
}
