package com.sourcevault.analysis.parser;

import com.sourcevault.common.dto.DiscoveredResourceDto;
import com.sourcevault.common.model.ResourceType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceDiscoveryParserTest {

    private final ResourceDiscoveryParser parser = new ResourceDiscoveryParser();

    @Test
    void testDiscoversHtmlAndAssets() {
        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Test Page</title>
                    <link rel="stylesheet" href="/assets/style.css">
                    <link rel="icon" href="/favicon.png">
                    <link rel="manifest" href="/manifest.json">
                    <script src="/js/app.js"></script>
                </head>
                <body>
                    <img src="https://cdn.example.com/logo.svg" alt="Logo">
                    <div style="background-image: url('/fonts/custom.woff2');"></div>
                </body>
                </html>
                """;

        URI baseUri = URI.create("https://mysite.com/");
        Document doc = Jsoup.parse(html, baseUri.toString());
        List<DiscoveredResourceDto> resources = parser.parseResources(doc, baseUri);

        assertThat(resources).hasSizeGreaterThanOrEqualTo(5);

        assertThat(resources).anyMatch(r -> r.getResourceType() == ResourceType.HTML && r.getUrl().equals("https://mysite.com/"));
        assertThat(resources).anyMatch(r -> r.getResourceType() == ResourceType.STYLESHEET && r.getUrl().endsWith("/assets/style.css"));
        assertThat(resources).anyMatch(r -> r.getResourceType() == ResourceType.SCRIPT && r.getUrl().endsWith("/js/app.js"));
        assertThat(resources).anyMatch(r -> r.getResourceType() == ResourceType.IMAGE && r.getUrl().contains("cdn.example.com/logo.svg") && r.isThirdParty());
        assertThat(resources).anyMatch(r -> r.getResourceType() == ResourceType.FONT && r.getUrl().endsWith("/fonts/custom.woff2"));
    }

    @Test
    void testExtractsSourceMappingUrl() {
        String jsContent = "console.log('hello');\n//# sourceMappingURL=app.js.map";
        URI scriptUri = URI.create("https://mysite.com/static/js/app.js");

        Optional<String> mapUrl = parser.extractSourceMappingUrl(jsContent, scriptUri);
        assertThat(mapUrl).isPresent();
        assertThat(mapUrl.get()).isEqualTo("https://mysite.com/static/js/app.js.map");
    }
}
