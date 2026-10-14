package com.sourcevault.common.security;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityUtilsTest {

    private final SsrfValidator validator = new SsrfValidator(false);

    @Test
    void testUrlNormalization() {
        URI uri = validator.validateAndNormalize("topbrains.com");
        assertThat(uri.getScheme()).isEqualTo("https");
        assertThat(uri.getHost()).isEqualTo("topbrains.com");

        URI httpUri = validator.validateAndNormalize("http://example.com/test");
        assertThat(httpUri.getScheme()).isEqualTo("http");
        assertThat(httpUri.getHost()).isEqualTo("example.com");
    }

    @Test
    void testRejectsIllegalProtocols() {
        assertThatThrownBy(() -> validator.validateAndNormalize("ftp://example.com"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validateAndNormalize("file:///etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validateAndNormalize("javascript:alert(1)"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testRejectsLocalhostAndMetadata() {
        assertThatThrownBy(() -> validator.validateAndNormalize("http://localhost:8080"))
                .isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> validator.validateAndNormalize("http://127.0.0.1/test"))
                .isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> validator.validateAndNormalize("http://169.254.169.254/latest/meta-data"))
                .isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> validator.validateAndNormalize("http://metadata.google.internal/computeMetadata/v1"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void testPathSanitization() {
        Path base = Paths.get("target", "test-output");
        Path safe = PathSanitizer.resolveSafe(base, "assets/app.js");
        assertThat(safe.startsWith(base)).isTrue();
        assertThat(safe.toString().replace('\\', '/')).contains("assets/app.js");

        assertThatThrownBy(() -> PathSanitizer.resolveSafe(base, "../../../etc/passwd"))
                .isInstanceOf(SecurityException.class);

        String clean = PathSanitizer.sanitizeFilename("foo/bar?param=1");
        assertThat(clean).doesNotContain("/").doesNotContain("?");
    }
}
