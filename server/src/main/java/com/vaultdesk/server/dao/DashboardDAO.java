package com.vaultdesk.server.dao;

import com.vaultdesk.server.model.Ticket;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class DashboardDAO {
    private final JdbcTemplate jdbc;

    public DashboardDAO(JdbcTemplate jdbc) {  // constructor injection
        this.jdbc =jdbc ;
    }

    public int getTotalAssets()
    {
        return jdbc.queryForObject("SELECT COUNT(*) FROM assets",Integer.class);
    }

    public int getOpenTicketsCount()
    {
        return jdbc.queryForObject("SELECT COUNT(*) FROM tickets WHERE status = 'Open'",Integer.class);
    }

    public int getExpiringLicensesCount()
    {
        return jdbc.queryForObject("SELECT COUNT(*) FROM licenses \n" +
                "WHERE expiry_date <= date('now', '+30 days') \n" +
                "AND expiry_date >= date('now')",Integer.class);
    }
    public int getTotalEmployees()
    {
        return jdbc.queryForObject("SELECT COUNT(*) FROM employees WHERE active = 1",Integer.class);
    }
    public List<Ticket> getRecentTickets()
    {

            List<Map<String,Object>> rows = jdbc.queryForList(
                    "SELECT * FROM tickets ORDER BY created_at DESC LIMIT 10");

            List<Ticket> tickets = new ArrayList<>();

            for (Map<String,Object> row : rows) {
                tickets.add(mapRowToTicket(row));
            }
            return tickets;

    }
    private Ticket mapRowToTicket(Map<String,Object> row) {
        return new Ticket(
                ((Number) row.get("id")).intValue(),
                (String) row.get("ticket_no"),
                (String) row.get("title"),
                (String) row.get("description"),
                (String) row.get("category"),
                (String) row.get("priority"),
                (String) row.get("status"),
                row.get("reported_by") != null ? ((Number) row.get("reported_by")).intValue() : 0,
                row.get("asset_id") != null ? ((Number) row.get("asset_id")).intValue() : 0,
                row.get("assigned_to") != null ? ((Number) row.get("assigned_to")).intValue() : 0,
                (String) row.get("created_at"),
                (String) row.get("updated_at"),
                (String) row.get("resolved_at"),
                (String) row.get("resolution")
        );
    }
}
