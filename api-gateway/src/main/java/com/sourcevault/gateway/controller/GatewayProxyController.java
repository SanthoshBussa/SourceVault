package com.sourcevault.gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.*;

@RestController
@CrossOrigin(origins = "*")
public class GatewayProxyController {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${sourcevault.services.analysis-url:http://localhost:8081}")
    private String analysisUrl;

    @Value("${sourcevault.services.collection-url:http://localhost:8082}")
    private String collectionUrl;

    @Value("${sourcevault.services.archive-url:http://localhost:8083}")
    private String archiveUrl;

    @Value("${sourcevault.services.job-url:http://localhost:8084}")
    private String jobUrl;

    @RequestMapping(value = {
            "/api/v1/analysis/**",
            "/api/v1/collection/**",
            "/api/v1/archive/**",
            "/api/v1/jobs/**"
    })
    public ResponseEntity<byte[]> proxyRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        String targetBaseUrl;

        if (path.startsWith("/api/v1/analysis")) {
            targetBaseUrl = analysisUrl;
        } else if (path.startsWith("/api/v1/collection")) {
            targetBaseUrl = collectionUrl;
        } else if (path.startsWith("/api/v1/archive") || path.matches("^/api/v1/jobs/[^/]+/(archive|manifest|failed-resources).*")) {
            targetBaseUrl = archiveUrl;
        } else {
            targetBaseUrl = jobUrl;
        }

        String targetUrl = targetBaseUrl + path;
        if (request.getQueryString() != null) {
            targetUrl += "?" + request.getQueryString();
        }

        try {
            HttpMethod method = HttpMethod.valueOf(request.getMethod());
            HttpHeaders headers = new HttpHeaders();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if (!headerName.equalsIgnoreCase("host") && !headerName.equalsIgnoreCase("content-length")) {
                    headers.addAll(headerName, Collections.list(request.getHeaders(headerName)));
                }
            }

            byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
            HttpEntity<byte[]> entity = new HttpEntity<>(body, headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    URI.create(targetUrl),
                    method,
                    entity,
                    byte[].class
            );

            HttpHeaders cleanHeaders = new HttpHeaders();
            response.getHeaders().forEach((k, v) -> {
                if (!k.equalsIgnoreCase("Transfer-Encoding")) {
                    cleanHeaders.put(k, v);
                }
            });

            return new ResponseEntity<>(response.getBody(), cleanHeaders, response.getStatusCode());

        } catch (HttpStatusCodeException hsce) {
            return ResponseEntity.status(hsce.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(hsce.getResponseBodyAsByteArray());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(("{\"error\":\"Bad Gateway\",\"message\":\"" + e.getMessage() + "\"}").getBytes());
        }
    }
}
