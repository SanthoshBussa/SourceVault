package com.sourcevault.server;

import com.sourcevault.common.dto.CreateJobRequest;
import com.sourcevault.common.dto.JobResponse;
import com.sourcevault.common.model.JobStatus;
import com.sourcevault.job.service.JobService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = SourceVaultApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FullPipelineIntegrationTest {

    private static HttpServer testServer;
    private static int testServerPort;

    @Autowired
    private JobService jobService;

    @BeforeAll
    static void startTestHttpServer() throws IOException {
        System.setProperty("sourcevault.security.allow-local", "true");

        testServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        testServerPort = testServer.getAddress().getPort();

        // 1. HTML index page
        testServer.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String path = exchange.getRequestURI().getPath();
                if (path.equals("/") || path.equals("/index.html")) {
                    String html = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <title>Integration Test Website</title>
                                <link rel="stylesheet" href="/style.css">
                                <link rel="icon" href="/favicon.png">
                                <script src="/app.js"></script>
                            </head>
                            <body>
                                <h1>Test Site</h1>
                                <img src="/logo.png" alt="Test Logo">
                                <img src="/missing.png" alt="Missing Resource">
                                <a href="/data.json">Data</a>
                            </body>
                            </html>
                            """;
                    sendResponse(exchange, 200, "text/html", html.getBytes(StandardCharsets.UTF_8));
                } else if (path.equals("/style.css")) {
                    String css = "@font-face { font-family: 'TestFont'; src: url('/font.woff2'); }\nbody { background: #fff; }";
                    sendResponse(exchange, 200, "text/css", css.getBytes(StandardCharsets.UTF_8));
                } else if (path.equals("/app.js")) {
                    String js = "console.log('App running');\n//# sourceMappingURL=app.js.map";
                    sendResponse(exchange, 200, "application/javascript", js.getBytes(StandardCharsets.UTF_8));
                } else if (path.equals("/app.js.map")) {
                    String map = """
                            {
                              "version": 3,
                              "file": "app.js",
                              "sources": ["webpack:///src/main.ts", "webpack:///src/utils.ts"],
                              "sourcesContent": [
                                "console.log('original main source');",
                                "export const calculate = () => 100;"
                              ],
                              "mappings": "AAAA"
                            }
                            """;
                    sendResponse(exchange, 200, "application/json", map.getBytes(StandardCharsets.UTF_8));
                } else if (path.equals("/logo.png") || path.equals("/favicon.png")) {
                    byte[] dummyPng = new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 1};
                    sendResponse(exchange, 200, "image/png", dummyPng);
                } else if (path.equals("/font.woff2")) {
                    byte[] dummyFont = "wOF2-dummy-font-data".getBytes(StandardCharsets.UTF_8);
                    sendResponse(exchange, 200, "font/woff2", dummyFont);
                } else if (path.equals("/data.json")) {
                    String json = "{\"status\":\"ok\",\"timestamp\":123456789}";
                    sendResponse(exchange, 200, "application/json", json.getBytes(StandardCharsets.UTF_8));
                } else if (path.equals("/missing.png")) {
                    // Deliberately 404
                    sendResponse(exchange, 404, "text/plain", "Not Found".getBytes(StandardCharsets.UTF_8));
                } else {
                    sendResponse(exchange, 404, "text/plain", "Not Found".getBytes(StandardCharsets.UTF_8));
                }
            }
        });

        testServer.start();
    }

    @AfterAll
    static void stopTestHttpServer() {
        if (testServer != null) {
            testServer.stop(0);
        }
        System.clearProperty("sourcevault.security.allow-local");
    }

    private static void sendResponse(HttpExchange exchange, int status, String contentType, byte[] data) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    @Test
    void testCompleteWorkflowPipeline() throws Exception {
        String testUrl = "http://127.0.0.1:" + testServerPort + "/";

        CreateJobRequest request = new CreateJobRequest();
        request.setWebsiteUrl(testUrl);
        request.setReconstructSourceMaps(true);
        request.setIncludeThirdParty(true);

        JobResponse initialJob = jobService.createAndStartJob(request, null);
        assertThat(initialJob).isNotNull();
        String jobId = initialJob.getId();

        // Wait for pipeline execution to complete
        JobResponse currentJob = null;
        for (int i = 0; i < 40; i++) {
            Thread.sleep(500);
            currentJob = jobService.getJob(jobId).orElse(null);
            if (currentJob != null &&
                    (currentJob.getStatus() == JobStatus.COMPLETED ||
                     currentJob.getStatus() == JobStatus.COMPLETED_WITH_WARNINGS ||
                     currentJob.getStatus() == JobStatus.FAILED)) {
                break;
            }
        }

        assertThat(currentJob).isNotNull();
        assertThat(currentJob.getStatus()).isIn(JobStatus.COMPLETED, JobStatus.COMPLETED_WITH_WARNINGS);

        // Check archive existence
        assertThat(currentJob.getArchivePath()).isNotNull();
        Path zipPath = Path.of(currentJob.getArchivePath());
        assertThat(Files.exists(zipPath)).isTrue();
        assertThat(Files.size(zipPath)).isGreaterThan(100);

        // Inspect ZIP entries
        List<String> entries = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            Enumeration<? extends ZipEntry> en = zipFile.entries();
            while (en.hasMoreElements()) {
                entries.add(en.nextElement().getName());
            }
        }

        // Verify manifest, readme, summary, and reconstructed sources
        assertThat(entries).anyMatch(e -> e.endsWith("manifest.csv"));
        assertThat(entries).anyMatch(e -> e.endsWith("README.md"));
        assertThat(entries).anyMatch(e -> e.endsWith("summary.json"));
        assertThat(entries).anyMatch(e -> e.endsWith("failed_resources.txt"));
        assertThat(entries).anyMatch(e -> e.contains("reconstructed_sources/src/main.ts"));
        assertThat(entries).anyMatch(e -> e.contains("reconstructed_sources/src/utils.ts"));

        // Verify the deliberately missing resource was tracked as failed
        assertThat(currentJob.getTotalFailed()).isGreaterThanOrEqualTo(1);
        assertThat(currentJob.getTotalDownloaded()).isGreaterThanOrEqualTo(5);
        assertThat(currentJob.getTotalReconstructed()).isEqualTo(2);
    }
}
