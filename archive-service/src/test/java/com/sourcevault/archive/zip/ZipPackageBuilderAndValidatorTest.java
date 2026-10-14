package com.sourcevault.archive.zip;

import com.sourcevault.archive.validator.ArchiveValidator;
import com.sourcevault.common.dto.ArchiveManifestItem;
import com.sourcevault.common.model.ResourceType;
import com.sourcevault.common.model.RetrievalMethod;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ZipPackageBuilderAndValidatorTest {

    private final ZipPackageBuilder zipBuilder = new ZipPackageBuilder();
    private final ArchiveValidator validator = new ArchiveValidator();

    private Path testDir;
    private Path stagingDir;
    private Path outputZip;

    @BeforeEach
    void setUp() throws Exception {
        testDir = Paths.get("target", "test-zip-" + System.currentTimeMillis());
        stagingDir = testDir.resolve("staging");
        outputZip = testDir.resolve("test_archive.zip");
        Files.createDirectories(stagingDir);
    }

    @AfterEach
    void tearDown() throws Exception {
        FileUtils.deleteDirectory(testDir.toFile());
    }

    @Test
    void testZipCreationAndIntegrityValidation() throws Exception {
        byte[] htmlBytes = "<html><body>Hello</body></html>".getBytes(StandardCharsets.UTF_8);
        byte[] cssBytes = "body { color: red; }".getBytes(StandardCharsets.UTF_8);

        Files.write(stagingDir.resolve("index.html"), htmlBytes);
        Files.createDirectories(stagingDir.resolve("assets"));
        Files.write(stagingDir.resolve("assets/style.css"), cssBytes);

        List<ArchiveManifestItem> manifest = new ArrayList<>();
        manifest.add(new ArchiveManifestItem(
                "https://example.com/index.html",
                "index.html",
                ResourceType.HTML,
                "text/html",
                200,
                htmlBytes.length,
                computeSha256(htmlBytes),
                RetrievalMethod.HTTP_PUBLIC,
                false
        ));
        manifest.add(new ArchiveManifestItem(
                "https://example.com/assets/style.css",
                "assets/style.css",
                ResourceType.STYLESHEET,
                "text/css",
                200,
                cssBytes.length,
                computeSha256(cssBytes),
                RetrievalMethod.HTTP_PUBLIC,
                false
        ));

        ZipPackageBuilder.ZipResult zipResult = zipBuilder.createZip(stagingDir, outputZip, "example_backup");

        assertThat(Files.exists(outputZip)).isTrue();
        assertThat(zipResult.getSizeBytes()).isGreaterThan(0);
        assertThat(zipResult.getTotalEntries()).isEqualTo(2);

        ArchiveValidator.ValidationReport report = validator.validateArchive(outputZip, manifest, "example_backup");
        assertThat(report.isValid()).isTrue();
        assertThat(report.getStatus()).isEqualTo("VALID");
        assertThat(report.getVerifiedEntries()).isEqualTo(2);
        assertThat(report.getIssues()).isEmpty();
    }

    private String computeSha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(data));
    }
}
