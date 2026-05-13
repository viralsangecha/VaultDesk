package com.vaultdesk.server.dao;

import com.vaultdesk.server.model.Ticket;
import com.vaultdesk.server.model.TicketComment;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TicketDAO {
    private final JdbcTemplate jdbc;

    public TicketDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
    private static final AtomicInteger series = new AtomicInteger(1);

    public List<Ticket> getAllTickets() {
        try {
            List<Map<String,Object>> rows = jdbc.queryForList("SELECT * FROM tickets");
            List<Ticket> tickets = new ArrayList<>();
            for (Map<String,Object> row : rows) {
                tickets.add(mapRowToTicket(row));
            }
            return tickets;
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public Ticket getTicketById(int id) {
        try {
            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT * FROM tickets where id=?", id);
            return mapRowToTicket(row);
        } catch (EmptyResultDataAccessException e) {
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

    public void saveTicket(String title, String description,
                           String category, String priority, int reportedBy) {
        String ticketNo = "SCL-" + Year.now().getValue()+"-"+ series.getAndIncrement();
        jdbc.update(
                "INSERT INTO tickets (ticket_no, title, description, category, " +
                        "priority, status, reported_by, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, 'Open', ?, datetime('now'))",
                ticketNo, title, description, category, priority, reportedBy
        );
    }

    public int updateTicketStatus(int id, String status, String resolution) {
        return jdbc.update(
                "UPDATE tickets SET " +
                        "status = ?, " +
                        "resolution = ?, " +
                        "updated_at = datetime('now'), " +
                        "resolved_at = CASE WHEN ? = 'Resolved' " +
                        "THEN datetime('now') ELSE resolved_at END " +
                        "WHERE id = ?",
                status, resolution, status, id);
    }

    public int assignTicket(int id, int userId) {
        return jdbc.update(
                "UPDATE tickets SET assigned_to = ?, updated_at = datetime('now') WHERE id = ?",
                userId, id);
    }

    public List<Ticket> getTicketsByDept(int deptId) {
        List<Map<String,Object>> rows = jdbc.queryForList(
                "SELECT t.* FROM tickets t " +
                        "JOIN employees e ON t.reported_by = e.id " +
                        "WHERE e.department_id = ? " +
                        "ORDER BY t.created_at DESC", deptId);
        List<Ticket> tickets = new ArrayList<>();
        for (Map<String,Object> row : rows) {
            tickets.add(mapRowToTicket(row));
        }
        return tickets;
    }

    // ── Comments ──────────────────────────────────────────

    public List<TicketComment> getComments(int ticketId) {
        List<Map<String,Object>> rows = jdbc.queryForList(
                "SELECT tc.*, u.full_name as added_by_name " +
                        "FROM ticket_comments tc " +
                        "LEFT JOIN users u ON tc.added_by = u.id " +
                        "WHERE tc.ticket_id = ? " +
                        "ORDER BY tc.added_at ASC", ticketId);
        List<TicketComment> comments = new ArrayList<>();
        for (Map<String,Object> row : rows) {
            comments.add(new TicketComment(
                    ((Number) row.get("id")).intValue(),
                    ((Number) row.get("ticket_id")).intValue(),
                    (String) row.get("comment"),
                    row.get("added_by") != null
                            ? ((Number) row.get("added_by")).intValue() : 0,
                    (String) row.get("added_by_name"),
                    (String) row.get("added_at")
            ));
        }
        return comments;
    }

    public void saveComment(int ticketId, String comment, int addedBy) {
        jdbc.update(
                "INSERT INTO ticket_comments (ticket_id, comment, added_by, added_at) " +
                        "VALUES (?, ?, ?, datetime('now'))",
                ticketId, comment, addedBy);
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
                row.get("reported_by") != null
                        ? ((Number) row.get("reported_by")).intValue() : 0,
                row.get("asset_id") != null
                        ? ((Number) row.get("asset_id")).intValue() : 0,
                row.get("assigned_to") != null
                        ? ((Number) row.get("assigned_to")).intValue() : 0,
                (String) row.get("created_at"),
                (String) row.get("updated_at"),
                (String) row.get("resolved_at"),
                (String) row.get("resolution")
        );
    }
}