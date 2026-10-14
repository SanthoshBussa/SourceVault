package com.sourcevault.analysis.parser;

import com.sourcevault.common.dto.DiscoveredResourceDto;
import com.sourcevault.common.model.ResourceType;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ResourceDiscoveryParser {

    private static final Logger log = LoggerFactory.getLogger(ResourceDiscoveryParser.class);

    private static final Pattern CSS_URL_PATTERN = Pattern.compile("url\\s*\\(\\s*['\"]?([^'\"\\)]+)['\"]?\\s*\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SOURCE_MAP_COMMENT_PATTERN = Pattern.compile("//[#@]\\s*sourceMappingURL=([\\S]+)|/\\*[#@]\\s*sourceMappingURL=([\\S]+?)\\s*\\*/");

    /**
     * Parses the HTML document and extracts all linked resources.
     */
    public List<DiscoveredResourceDto> parseResources(Document doc, URI baseUri) {
        Map<String, DiscoveredResourceDto> discoveredMap = new LinkedHashMap<>();
        String baseOrigin = getOrigin(baseUri);

        // 1. Root HTML page
        DiscoveredResourceDto rootHtml = new DiscoveredResourceDto(
                baseUri.toString(),
                ResourceType.HTML,
                baseOrigin,
                baseUri.getPath().isEmpty() ? "/index.html" : baseUri.getPath()
        );
        rootHtml.setThirdParty(false);
        discoveredMap.put(baseUri.toString(), rootHtml);

        // 2. Scripts: <script src="...">
        Elements scripts = doc.select("script[src]");
        for (Element script : scripts) {
            String absUrl = script.attr("abs:src");
            if (isValidHttpUrl(absUrl)) {
                addResource(discoveredMap, absUrl, ResourceType.SCRIPT, baseOrigin);
            }
        }

        // 3. Stylesheets: <link rel="stylesheet" href="...">
        Elements stylesheets = doc.select("link[rel~=stylesheet]");
        for (Element link : stylesheets) {
            String absUrl = link.attr("abs:href");
            if (isValidHttpUrl(absUrl)) {
                addResource(discoveredMap, absUrl, ResourceType.STYLESHEET, baseOrigin);
            }
        }

        // 4. Icons & Favicon: <link rel="icon">, <link rel="apple-touch-icon">
        Elements icons = doc.select("link[rel*='icon']");
        for (Element icon : icons) {
            String absUrl = icon.attr("abs:href");
            if (isValidHttpUrl(absUrl)) {
                addResource(discoveredMap, absUrl, ResourceType.IMAGE, baseOrigin);
            }
        }

        // 5. Manifest: <link rel="manifest">
        Elements manifests = doc.select("link[rel='manifest']");
        for (Element manifest : manifests) {
            String absUrl = manifest.attr("abs:href");
            if (isValidHttpUrl(absUrl)) {
                addResource(discoveredMap, absUrl, ResourceType.MANIFEST, baseOrigin);
            }
        }

        // 6. Preload / Prefetch resources: <link rel="preload" as="..." href="...">
        Elements preloads = doc.select("link[rel='preload'], link[rel='prefetch'], link[rel='modulepreload']");
        for (Element preload : preloads) {
            String absUrl = preload.attr("abs:href");
            if (isValidHttpUrl(absUrl)) {
                String asAttr = preload.attr("as").toLowerCase(Locale.ROOT);
                ResourceType type = determineTypeFromAsAttr(asAttr, absUrl);
                addResource(discoveredMap, absUrl, type, baseOrigin);
            }
        }

        // 7. Images: <img src="...">, <img srcset="...">, <picture><source srcset="...">
        Elements imgs = doc.select("img[src]");
        for (Element img : imgs) {
            String absUrl = img.attr("abs:src");
            if (isValidHttpUrl(absUrl)) {
                addResource(discoveredMap, absUrl, ResourceType.IMAGE, baseOrigin);
            }
        }

        Elements srcsets = doc.select("img[srcset], source[srcset]");
        for (Element elem : srcsets) {
            String srcset = elem.attr("srcset");
            for (String part : srcset.split(",")) {
                String candidate = part.trim().split("\\s+")[0];
                if (!candidate.isEmpty()) {
                    String absUrl = resolveRelative(baseUri, candidate);
                    if (isValidHttpUrl(absUrl)) {
                        addResource(discoveredMap, absUrl, ResourceType.IMAGE, baseOrigin);
                    }
                }
            }
        }

        // 8. Fonts: <link href="..."> matching font extensions
        Elements allLinks = doc.select("link[href]");
        for (Element link : allLinks) {
            String absUrl = link.attr("abs:href");
            if (isValidHttpUrl(absUrl) && isFontUrl(absUrl)) {
                addResource(discoveredMap, absUrl, ResourceType.FONT, baseOrigin);
            }
        }

        // 9. Inspect inline style tags and style attributes for url(...)
        Elements styles = doc.select("style, [style]");
        for (Element style : styles) {
            String content = style.is("style") ? style.data() : style.attr("style");
            Matcher matcher = CSS_URL_PATTERN.matcher(content);
            while (matcher.find()) {
                String rawRef = matcher.group(1).trim();
                if (!rawRef.startsWith("data:")) {
                    String absUrl = resolveRelative(baseUri, rawRef);
                    if (isValidHttpUrl(absUrl)) {
                        ResourceType type = isFontUrl(absUrl) ? ResourceType.FONT : ResourceType.IMAGE;
                        addResource(discoveredMap, absUrl, type, baseOrigin);
                    }
                }
            }
        }

        return new ArrayList<>(discoveredMap.values());
    }

    /**
     * Extracts source map references from JavaScript or CSS content.
     */
    public Optional<String> extractSourceMappingUrl(String content, URI resourceUri) {
        if (content == null || content.isEmpty()) {
            return Optional.empty();
        }

        Matcher matcher = SOURCE_MAP_COMMENT_PATTERN.matcher(content);
        String lastFound = null;
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                lastFound = matcher.group(1).trim();
            } else if (matcher.group(2) != null) {
                lastFound = matcher.group(2).trim();
            }
        }

        if (lastFound != null) {
            if (lastFound.startsWith("data:")) {
                return Optional.of(lastFound);
            }
            String resolved = resolveRelative(resourceUri, lastFound);
            return Optional.ofNullable(resolved);
        }

        return Optional.empty();
    }

    public ResourceType classifyByUrl(String url) {
        String path = url.split("[?#]")[0].toLowerCase(Locale.ROOT);
        if (path.endsWith(".js") || path.endsWith(".mjs") || path.endsWith(".cjs")) {
            return ResourceType.SCRIPT;
        } else if (path.endsWith(".css")) {
            return ResourceType.STYLESHEET;
        } else if (path.endsWith(".map")) {
            return ResourceType.SOURCE_MAP;
        } else if (path.endsWith(".json")) {
            return ResourceType.JSON;
        } else if (path.endsWith(".webmanifest") || path.endsWith("manifest.json")) {
            return ResourceType.MANIFEST;
        } else if (isFontUrl(path)) {
            return ResourceType.FONT;
        } else if (isImageUrl(path)) {
            return ResourceType.IMAGE;
        } else if (path.endsWith(".html") || path.endsWith(".htm") || path.endsWith("/")) {
            return ResourceType.HTML;
        }
        return ResourceType.OTHER;
    }

    private void addResource(Map<String, DiscoveredResourceDto> map, String url, ResourceType type, String baseOrigin) {
        if (url == null || url.trim().isEmpty() || url.startsWith("data:") || map.containsKey(url)) {
            return;
        }

        try {
            URI uri = URI.create(url).normalize();
            String origin = getOrigin(uri);
            String path = uri.getPath() == null || uri.getPath().isEmpty() ? "/" : uri.getPath();

            DiscoveredResourceDto dto = new DiscoveredResourceDto(url, type, origin, path);
            dto.setThirdParty(!origin.equalsIgnoreCase(baseOrigin));
            map.put(url, dto);
        } catch (Exception e) {
            log.warn("Could not normalize resource URL: {}", url);
        }
    }

    private ResourceType determineTypeFromAsAttr(String asAttr, String url) {
        return switch (asAttr) {
            case "script" -> ResourceType.SCRIPT;
            case "style" -> ResourceType.STYLESHEET;
            case "image" -> ResourceType.IMAGE;
            case "font" -> ResourceType.FONT;
            default -> classifyByUrl(url);
        };
    }

    private boolean isFontUrl(String url) {
        String lower = url.split("[?#]")[0].toLowerCase(Locale.ROOT);
        return lower.endsWith(".woff2") || lower.endsWith(".woff") ||
               lower.endsWith(".ttf") || lower.endsWith(".otf") || lower.endsWith(".eot");
    }

    private boolean isImageUrl(String url) {
        String lower = url.split("[?#]")[0].toLowerCase(Locale.ROOT);
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
               lower.endsWith(".svg") || lower.endsWith(".gif") || lower.endsWith(".webp") ||
               lower.endsWith(".ico") || lower.endsWith(".avif");
    }

    private String getOrigin(URI uri) {
        String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase(Locale.ROOT) : "https";
        String host = uri.getHost() != null ? uri.getHost().toLowerCase(Locale.ROOT) : "";
        int port = uri.getPort();

        if (port == -1 || (port == 80 && "http".equals(scheme)) || (port == 443 && "https".equals(scheme))) {
            return scheme + "://" + host;
        }
        return scheme + "://" + host + ":" + port;
    }

    private String resolveRelative(URI baseUri, String relativeUrl) {
        try {
            return baseUri.resolve(relativeUrl.trim()).normalize().toString();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isValidHttpUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }
}
