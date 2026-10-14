package com.sourcevault.archive.sourcemap;

import com.sourcevault.common.dto.ArchiveManifestItem;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class SourceMapReconstructorTest {

    private final SourceMapReconstructor reconstructor = new SourceMapReconstructor();
    private Path tempStaging;

    @BeforeEach
    void setUp() throws Exception {
        tempStaging = Paths.get("target", "test-staging-" + System.currentTimeMillis());
        Files.createDirectories(tempStaging);
    }

    @AfterEach
    void tearDown() throws Exception {
        FileUtils.deleteDirectory(tempStaging.toFile());
    }

    @Test
    void testReconstructsSourcesWhenSourcesContentPresent() throws Exception {
        String mapJson = """
                {
                  "version": 3,
                  "file": "bundle.js",
                  "sources": ["webpack:///src/App.tsx", "webpack:///src/utils/math.ts"],
                  "sourcesContent": [
                    "export const App = () => <div>Hello World</div>;",
                    "export const add = (a, b) => a + b;"
                  ],
                  "mappings": "AAAA"
                }
                """;

        SourceMapReconstructor.ReconstructionResult result =
                reconstructor.reconstruct(mapJson.getBytes(StandardCharsets.UTF_8), "https://example.com/bundle.js.map", tempStaging);

        assertThat(result.getReconstructedCount()).isEqualTo(2);
        assertThat(result.getReconstructedItems()).hasSize(2);

        Path appFile = tempStaging.resolve("reconstructed_sources/src/App.tsx");
        assertThat(Files.exists(appFile)).isTrue();
        assertThat(Files.readString(appFile)).contains("Hello World");

        Path mathFile = tempStaging.resolve("reconstructed_sources/src/utils/math.ts");
        assertThat(Files.exists(mathFile)).isTrue();
        assertThat(Files.readString(mathFile)).contains("export const add");
    }

    @Test
    void testDoesNotReconstructWhenSourcesContentMissing() {
        String mapJson = """
                {
                  "version": 3,
                  "file": "bundle.js",
                  "sources": ["src/App.tsx"],
                  "mappings": "AAAA"
                }
                """;

        SourceMapReconstructor.ReconstructionResult result =
                reconstructor.reconstruct(mapJson.getBytes(StandardCharsets.UTF_8), "https://example.com/bundle.js.map", tempStaging);

        assertThat(result.getReconstructedCount()).isZero();
        assertThat(result.getReconstructedItems()).isEmpty();
    }
}
