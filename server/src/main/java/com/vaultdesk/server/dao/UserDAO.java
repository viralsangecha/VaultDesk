package com.vaultdesk.server.dao;

import com.vaultdesk.server.model.User;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class UserDAO {
    private final JdbcTemplate jdbc;

    public UserDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public User getUserByUsername(String username) {
        try {
            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT * FROM users WHERE username = ?", username);
            return mapRow(row);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public User getUserById(int id) {
        try {
            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT * FROM users WHERE id = ?", id);
            return mapRow(row);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public boolean validateLogin(String username, String passwordHash) {
        try {
            jdbc.queryForMap(
                    "SELECT * FROM users WHERE username=? AND password_hash=? AND active=1",
                    username, passwordHash);
            return true;
        } catch (EmptyResultDataAccessException e) {
            return false;
        }
    }
    public List<User> getAllUsersAdmin() {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM users");
        List<User> users = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            users.add(mapRow(row));
        }
        return users;
    }

    public List<User> getAllUsers() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM users WHERE active = 1");
        List<User> users = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            users.add(mapRow(row));
        }
        return users;
    }


    public void saveUser(String username, String passwordHash,
                         String fullName, String role, int deptId,String email) {
        jdbc.update(
                "INSERT INTO users (username, password_hash, full_name, " +
                        "role, active, dept_id, created_at,email) " +
                        "VALUES (?, ?, ?, ?, 1, ?, datetime('now'),?)",
                username, passwordHash, fullName, role, deptId,email
        );
    }


    public int deactivateUser(int id) {
        return jdbc.update(
                "UPDATE users SET active = 0 WHERE id = ?", id);
    }

    public int reactivateUser(int id) {
        return jdbc.update("UPDATE users SET active = 1 WHERE id = ?", id);
    }

    public int updateUser(int id, String fullName,String username, String email, String role, int deptId) {
        return jdbc.update(
                "UPDATE users SET full_name = ?,username = ?, email = ?, role = ?, dept_id = ? WHERE id = ?",
                fullName,username, email, role, deptId, id
        );
    }
    public boolean changePassword(int userId,
                                  String currentHash,
                                  String newHash) {
        // Verify current password first
        try {
            jdbc.queryForMap(
                    "SELECT id FROM users WHERE id = ? " +
                            "AND password_hash = ? AND active = 1",
                    userId, currentHash);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return false; // wrong current password
        }
        jdbc.update(
                "UPDATE users SET password_hash = ? WHERE id = ?",
                newHash, userId);
        return true;
    }
    public void updateLastLogin(int userId) {
        try {
            jdbc.update(
                    "UPDATE users SET last_login = datetime('now') WHERE id = ?",
                    userId);
        } catch (Exception e) {
            // ignore if column doesn't exist yet
        }
    }

    public void forceUpdatePassword(int userId, String newHash) {
        jdbc.update("UPDATE users SET password_hash = ? WHERE id = ?", newHash, userId);
    }

    public List<String> getAdminEmails() {
        List<String> emails = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT email FROM users WHERE active = 1 AND UPPER(role)=?",
                    "ADMIN"
            );
            for (Map<String, Object> row : rows) {
                String email = (String) row.get("email");
                if (email != null && !email.isEmpty()) {
                    emails.add(email);
                }
            }
        } catch (Exception e) {
            System.out.println("Error getting admin emails: "
                    + e.getMessage());
        }
        return emails;
    }

    private User mapRow(Map<String, Object> row) {
        return new User(
                ((Number) row.get("id")).intValue(),
                (String) row.get("username"),
                (String) row.get("password_hash"),
                (String) row.get("full_name"),
                (String) row.get("role"),
                ((Number) row.get("active")).intValue(),
                (String) row.get("created_at"),
                (String) row.get("last_login"),
                row.get("dept_id") != null
                        ? ((Number) row.get("dept_id")).intValue() : 0
                ,(String) row.get("email")
        );

    }
}