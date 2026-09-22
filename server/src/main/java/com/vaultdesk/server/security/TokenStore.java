package com.vaultdesk.server.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenStore {
    public record TokenInfo(int subjectId, String subjectType, String role, long expiresAtMillis) {}

    private final Map<String, TokenInfo> tokens = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private static final long TTL_MILLIS = 12L * 60 * 60 * 1000; // 12 hours

    public String issue(int subjectId, String subjectType, String role) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.put(token, new TokenInfo(subjectId, subjectType, role, System.currentTimeMillis() + TTL_MILLIS));
        return token;
    }

    public TokenInfo validate(String token) {
        if (token == null) return null;
        TokenInfo info = tokens.get(token);
        if (info == null) return null;
        if (System.currentTimeMillis() > info.expiresAtMillis()) {
            tokens.remove(token);
            return null;
        }
        return info;
    }

    public void invalidate(String token) {
        if (token != null) tokens.remove(token);
    }
}