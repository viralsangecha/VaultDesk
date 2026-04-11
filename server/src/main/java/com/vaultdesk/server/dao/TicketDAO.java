package com.vaultdesk.server.dao;

import com.vaultdesk.server.model.Ticket;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;



@Component
public class TicketDAO {
    private final JdbcTemplate jdbc;

    public TicketDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }


    public List<Ticket> getAllTickets()
    {
        try {
            List<Map<String,Object>> rows = jdbc.queryForList(
                    "SELECT * FROM tickets");

            List<Ticket> tickets = new ArrayList<>();

            for (Map<String,Object> row : rows) {
                tickets.add(mapRowToTicket(row));
            }
            return tickets;
        }
        catch (EmptyResultDataAccessException e)
        {
            return null;
        }
    }

    public Ticket  getTicketById(int id)
    {
        try {
            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT * FROM tickets where id=?", id);
            return mapRowToTicket(row);
        }
        catch (EmptyResultDataAccessException e)
        {
            return null;
        }
    }

    public List<Ticket> getOpenTickets() {
        List<Map<String,Object>> rows = jdbc.queryForList(
                "SELECT * FROM tickets WHERE status = 'Open' ORDER BY created_at DESC");
        List<Ticket> tickets = new ArrayList<>();
        for (Map<String,Object> row : rows) {
            tickets.add(mapRowToTicket(row));
        }
        return tickets;
    }

    public List<Ticket> getTicketsByAssignee(int userId) {
        List<Map<String,Object>> rows = jdbc.queryForList(
                "SELECT * FROM tickets WHERE assigned_to = ? ORDER BY created_at DESC",
                userId);
        List<Ticket> tickets = new ArrayList<>();
        for (Map<String,Object> row : rows) {
            tickets.add(mapRowToTicket(row));
        }
        return tickets;
    }

    public void saveTicket(String title, String description,String category, String priority,int reportedBy)
    {
        String ticketNo = "VD-2025-" + System.currentTimeMillis();
        jdbc.update(
                "INSERT INTO tickets (ticket_no, title, description, category, " +
                        "priority, status, reported_by, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, 'Open', ?, datetime('now'))",
                ticketNo, title, description, category, priority, reportedBy
        );
    }

    public int updateTicketStatus(int id, String status, String resolution)
    {
        return jdbc.update("UPDATE tickets SET\n" +
                "     status = ?,\n" +
                "     resolution = ?,\n" +
                "     updated_at = datetime('now'),\n" +
                "     resolved_at = CASE WHEN ? = 'Resolved'\n" +
                "                   THEN datetime('now')\n" +
                "                   ELSE resolved_at END\n" +
                "     WHERE id = ?",status,resolution,status,id);
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
    public int assignTicket(int id, int userId) {
        return jdbc.update(
                "UPDATE tickets SET assigned_to = ?, updated_at = datetime('now') WHERE id = ?",
                userId, id
        );
    }

}
