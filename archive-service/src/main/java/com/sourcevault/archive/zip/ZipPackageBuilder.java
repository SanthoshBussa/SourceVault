package com.sourcevault.archive.zip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class ZipPackageBuilder {

    private static final Logger log = LoggerFactory.getLogger(ZipPackageBuilder.class);

    public static class ZipResult {
        private final Path zipPath;
        private final long sizeBytes;
        private final String sha256Checksum;
        private final int totalEntries;

        public ZipResult(Path zipPath, long sizeBytes, String sha256Checksum, int totalEntries) {
            this.zipPath = zipPath;
            this.sizeBytes = sizeBytes;
            this.sha256Checksum = sha256Checksum;
            this.totalEntries = totalEntries;
        }

        public Path getZipPath() { return zipPath; }
        public long getSizeBytes() { return sizeBytes; }
        public String getSha256Checksum() { return sha256Checksum; }
        public int getTotalEntries() { return totalEntries; }
    }

    /**
     * Packages all files in stagingDir into targetZipPath.
     */
    public ZipResult createZip(Path stagingDir, Path targetZipPath, String rootFolderName) throws IOException {
        if (!Files.exists(stagingDir)) {
            throw new IllegalArgumentException("Staging directory does not exist: " + stagingDir);
        }

        if (targetZipPath.getParent() != null) {
            Files.createDirectories(targetZipPath.getParent());
        }

        int entriesCount = 0;
        try (FileOutputStream fos = new FileOutputStream(targetZipPath.toFile());
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZipOutputStream zos = new ZipOutputStream(bos)) {

            zos.setLevel(ZipOutputStream.DEFLATED);

            try (Stream<Path> paths = Files.walk(stagingDir)) {
                for (Path file : (Iterable<Path>) paths::iterator) {
                    if (Files.isDirectory(file)) {
                        continue;
                    }

                    String relative = stagingDir.relativize(file).toString().replace('\\', '/');
                    String entryName = (rootFolderName != null && !rootFolderName.isEmpty())
                            ? rootFolderName + "/" + relative
                            : relative;

                    ZipEntry zipEntry = new ZipEntry(entryName);
                    zipEntry.setTime(Files.getLastModifiedTime(file).toMillis());
                    zos.putNextEntry(zipEntry);

                    Files.copy(file, zos);
                    zos.closeEntry();
                    entriesCount++;
                }
            }
            zos.finish();
        }

        long sizeBytes = Files.size(targetZipPath);
        String checksum = computeFileSha256(targetZipPath);

        return new ZipResult(targetZipPath, sizeBytes, checksum, entriesCount);
    }

    private String computeFileSha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (FileInputStream fis = new FileInputStream(file.toFile())) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            throw new IOException("Failed to compute SHA-256 checksum", e);
        }
    }
}
