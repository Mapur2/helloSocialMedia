package com.chatservice.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Session-scoped registry: maps STOMP sessionId -> userId captured on
 * the CONNECT frame. Spring's accessor.setUser() on the CONNECT frame does
 * not persist to subsequent SEND/SUBSCRIBE frames in the same session
 * reliably, so we keep our own lookup keyed by the session id that Spring
 * assigns on CONNECT.
 */
@Component
public class SessionRegistry {
    private final Map<String, String> sessionIdToUserId = new ConcurrentHashMap<>();

    public void bind(String sessionId, String userId) {
        if (sessionId != null && userId != null) {
            sessionIdToUserId.put(sessionId, userId);
        }
    }

    public String lookup(String sessionId) {
        return sessionId == null ? null : sessionIdToUserId.get(sessionId);
    }

    public void unbind(String sessionId) {
        if (sessionId != null) {
            sessionIdToUserId.remove(sessionId);
        }
    }
}