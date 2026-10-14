package com.sourcevault.gateway.controller;

import com.sourcevault.common.dto.BrowserResourceDto;
import com.sourcevault.common.dto.BrowserSessionDto;
import com.sourcevault.gateway.browser.BrowserSessionRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/browser")
@CrossOrigin(origins = "*")
public class BrowserCollectorController {

    private final BrowserSessionRegistry registry;

    public BrowserCollectorController(BrowserSessionRegistry registry) {
        this.registry = registry;
    }

    @PostMapping("/sessions")
    public ResponseEntity<BrowserSessionDto> registerSession(@RequestBody BrowserSessionDto dto) {
        BrowserSessionDto session = registry.registerSession(dto);
        return ResponseEntity.ok(session);
    }

    @PostMapping("/resources")
    public ResponseEntity<Map<String, Object>> submitResources(
            @RequestParam(required = false) String sessionId,
            @RequestBody List<BrowserResourceDto> resources) {

        String targetSession = (sessionId != null && !sessionId.isEmpty())
                ? sessionId
                : (String) registry.getStatus().get("sessionId");

        if (targetSession == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "No active browser session registered."));
        }

        boolean success = registry.submitResources(targetSession, resources);
        return ResponseEntity.ok(Map.of("success", success, "count", resources != null ? resources.size() : 0));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(registry.getStatus());
    }
}
