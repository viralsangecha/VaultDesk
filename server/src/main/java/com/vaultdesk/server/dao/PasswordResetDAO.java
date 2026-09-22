package com.vaultdesk.server.dao;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

@Component
public class PasswordResetDAO {
    private final JdbcTemplate jdbc;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record TokenInfo(String subjectType, int subjectId) {}

    public String createToken(String subjectType, int subjectId) {
        // Invalidate any previous unused tokens for this subject first
        jdbc.update("UPDATE password_reset_tokens SET used = 1 WHERE subject_type = ? AND subject_id = ? AND used = 0",
                subjectType, subjectId);

        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        jdbc.update(
                "INSERT INTO password_reset_tokens (subject_type, subject_id, token, expires_at) " +
                        "VALUES (?, ?, ?, datetime('now','+5 hours','+30 minutes','+30 minutes'))",
                subjectType, subjectId, token);
        return token;
    }

    public TokenInfo validateToken(String token) {
        try {
            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT * FROM password_reset_tokens WHERE token = ? AND used = 0 " +
                            "AND expires_at > datetime('now','+5 hours','+30 minutes')",
                    token);
            return new TokenInfo((String) row.get("subject_type"), ((Number) row.get("subject_id")).intValue());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public void markUsed(String token) {
        jdbc.update("UPDATE password_reset_tokens SET used = 1 WHERE token = ?", token);
    }
}