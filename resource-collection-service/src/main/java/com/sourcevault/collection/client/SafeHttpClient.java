package com.sourcevault.collection.client;

import com.sourcevault.common.security.SsrfValidator;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class SafeHttpClient {

    private static final Logger log = LoggerFactory.getLogger(SafeHttpClient.class);

    private final SsrfValidator ssrfValidator;

    @Value("${sourcevault.collection.connect-timeout-ms:10000}")
    private int connectTimeoutMs;

    @Value("${sourcevault.collection.read-timeout-ms:15000}")
    private int readTimeoutMs;

    @Value("${sourcevault.collection.max-file-size-bytes:20971520}")
    private long maxFileSizeBytes;

    @Value("${sourcevault.collection.user-agent:SourceVault-Collector/1.0}")
    private String userAgent;

    public SafeHttpClient() {
        this.ssrfValidator = new SsrfValidator();
    }

    public SafeHttpClient(SsrfValidator ssrfValidator) {
        this.ssrfValidator = ssrfValidator;
    }

    public static class DownloadResult {
        private final int statusCode;
        private final String contentType;
        private final byte[] data;
        private final String checksumSha256;
        private final long size;

        public DownloadResult(int statusCode, String contentType, byte[] data, String checksumSha256) {
            this.statusCode = statusCode;
            this.contentType = contentType;
            this.data = data;
            this.checksumSha256 = checksumSha256;
            this.size = data != null ? data.length : 0;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getContentType() {
            return contentType;
        }

        public byte[] getData() {
            return data;
        }

        public String getChecksumSha256() {
            return checksumSha256;
        }

        public long getSize() {
            return size;
        }
    }

    /**
     * Safely downloads resource bytes with SSRF validation, size limits, and checksum computation.
     */
    public DownloadResult downloadResource(String resourceUrl) throws Exception {
        URI targetUri = ssrfValidator.validateAndNormalize(resourceUrl);

        int redirects = 0;
        while (redirects < 5) {
            URL url = targetUri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(connectTimeoutMs);
            conn.setReadTimeout(readTimeoutMs);
            conn.setRequestProperty("User-Agent", userAgent);
            conn.setRequestProperty("Accept", "*/*");

            int status = conn.getResponseCode();

            // Handle safe redirect
            if (status >= 300 && status < 400) {
                String loc = conn.getHeaderField("Location");
                if (loc == null || loc.isEmpty()) {
                    throw new IllegalArgumentException("Redirect missing Location header: " + resourceUrl);
                }
                targetUri = targetUri.resolve(loc).normalize();
                ssrfValidator.validateHost(targetUri.getHost());
                redirects++;
                continue;
            }

            String contentType = conn.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            long contentLength = conn.getContentLengthLong();
            if (contentLength > maxFileSizeBytes) {
                throw new IllegalStateException("File size (" + contentLength + " bytes) exceeds limit of " + maxFileSizeBytes);
            }

            InputStream in = (status >= 200 && status < 400) ? conn.getInputStream() : conn.getErrorStream();
            if (in == null) {
                return new DownloadResult(status, contentType, new byte[0], "");
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] buffer = new byte[8192];
            int read;
            long totalRead = 0;
            try (in) {
                while ((read = in.read(buffer)) != -1) {
                    totalRead += read;
                    if (totalRead > maxFileSizeBytes) {
                        throw new IllegalStateException("Downloaded data exceeded file size limit of " + maxFileSizeBytes + " bytes");
                    }
                    baos.write(buffer, 0, read);
                    digest.update(buffer, 0, read);
                }
            }

            byte[] data = baos.toByteArray();
            String checksum = HexFormat.of().formatHex(digest.digest());

            return new DownloadResult(status, contentType, data, checksum);
        }

        throw new IllegalStateException("Too many redirects while downloading: " + resourceUrl);
    }
}
