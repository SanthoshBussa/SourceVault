package com.sourcevault.gateway.browser;

import com.sourcevault.common.dto.BrowserResourceDto;
import com.sourcevault.common.dto.BrowserSessionDto;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BrowserSessionRegistry {

    private final Map<String, BrowserSessionDto> sessions = new ConcurrentHashMap<>();

    public BrowserSessionDto registerSession(BrowserSessionDto dto) {
        if (dto.getSessionId() == null || dto.getSessionId().isEmpty()) {
            dto.setSessionId("browser_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10));
        }
        dto.setConnectedAt(Instant.now());
        dto.setLastPingAt(Instant.now());
        dto.setActive(true);
        sessions.put(dto.getSessionId(), dto);
        return dto;
    }

    public boolean submitResources(String sessionId, List<BrowserResourceDto> resources) {
        BrowserSessionDto session = sessions.get(sessionId);
        if (session != null) {
            session.setLastPingAt(Instant.now());
            session.getPendingResources().addAll(resources);
            return true;
        }
        return false;
    }

    public Map<String, Object> getStatus() {
        cleanExpiredSessions();
        boolean connected = sessions.values().stream().anyMatch(BrowserSessionDto::isActive);
        BrowserSessionDto activeSession = sessions.values().stream()
                .filter(BrowserSessionDto::isActive)
                .max(Comparator.comparing(BrowserSessionDto::getLastPingAt))
                .orElse(null);

        Map<String, Object> status = new HashMap<>();
        status.put("connected", connected);
        status.put("activeSessionCount", sessions.size());
        if (activeSession != null) {
            status.put("sessionId", activeSession.getSessionId());
            status.put("browserName", activeSession.getBrowserName());
            status.put("extensionVersion", activeSession.getExtensionVersion());
            status.put("activeTabUrl", activeSession.getActiveTabUrl());
            status.put("activeTabTitle", activeSession.getActiveTabTitle());
            status.put("lastPingAt", activeSession.getLastPingAt().toString());
            status.put("capturedResourcesCount", activeSession.getPendingResources().size());
        }
        return status;
    }

    public List<BrowserResourceDto> consumeResources(String sessionId) {
        BrowserSessionDto session = sessions.get(sessionId);
        if (session != null) {
            List<BrowserResourceDto> list = new ArrayList<>(session.getPendingResources());
            session.getPendingResources().clear();
            return list;
        }
        return Collections.emptyList();
    }

    public Optional<BrowserSessionDto> findSessionForUrl(String url) {
        cleanExpiredSessions();
        return sessions.values().stream()
                .filter(s -> s.isActive() && s.getActiveTabUrl() != null && url.startsWith(s.getActiveTabUrl()))
                .findFirst();
    }

    private void cleanExpiredSessions() {
        Instant expiry = Instant.now().minusSeconds(120); // 2 minutes heartbeat timeout
        sessions.entrySet().removeIf(e -> e.getValue().getLastPingAt().isBefore(expiry));
    }
}
