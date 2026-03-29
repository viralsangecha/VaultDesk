package com.vaultdesk.server.dao;

import com.vaultdesk.server.model.User;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UserDAO {
    private final JdbcTemplate jdbc;

    public UserDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public User getUserByUsername(String username)
    {
        try {
            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT * FROM users WHERE username = ?", username);

            return new User(
                    ((Number) row.get("id")).intValue(),
                    (String) row.get("username"),
                    (String) row.get("password_hash"),
                    (String) row.get("full_name"),
                    (String) row.get("role"),
                    ((Number) row.get("active")).intValue(),
                    (String) row.get("created_at")
            );
        }
        catch (EmptyResultDataAccessException e)
        {
           return null;
        }
    }

    public boolean validateLogin(String username, String passwordHash)
    {
        try {
            jdbc.queryForMap(
                    "SELECT * FROM users WHERE username=? AND password_hash=? AND active=1",
                    username, passwordHash
            );
            return true;  // row found = login valid
        } catch (EmptyResultDataAccessException e) {
            return false; // row not found = login invalid
        }
    }

}
