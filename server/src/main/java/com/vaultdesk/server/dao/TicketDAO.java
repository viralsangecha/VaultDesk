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
    private static final String IST_NOW = "datetime('now','+5 hours','+30 minutes')";

    public TicketDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
    private static final AtomicInteger series = new AtomicInteger(1);

    public List<Ticket> getAllTickets() {
        try {
            List<Map<String,Object>> rows = jdbc.queryForList("SELECT * FROM tickets ORDER BY id DESC;");
            List<Ticket> tickets = new ArrayList<>();
            for (Map<String,Object> row : rows) {
                tickets.add(mapRowToTicket(row));
            }
            return tickets;
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private void logStatusChange(int ticketId, String fromStatus, String toStatus, int changedBy, String reason) {
        jdbc.update(
                "INSERT INTO ticket_status_history (ticket_id, from_status, to_status, changed_by, changed_at, reason) " +
                        "VALUES (?, ?, ?, ?, " + IST_NOW + ", ?)",
                ticketId, fromStatus, toStatus, changedBy, reason);
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

    public List<Ticket> getTicketsByAssigneeByDept(int userId, int selDeptId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT t.* FROM tickets t " +
                        "JOIN employees e ON t.reported_by = e.id " +
                        "WHERE t.assigned_to = ? AND e.department_id = ? " +
                        "ORDER BY t.created_at DESC",
                userId, selDeptId);
        List<Ticket> tickets = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            tickets.add(mapRowToTicket(row));
        }
        return tickets;
    }

    public void saveTicket(String title, String description, String category,
                           String priority, int reportedBy,
                           String department, int assetId) {
        String sql = "SELECT MAX(id) FROM tickets";
        Integer maxSeries = jdbc.queryForObject(sql, Integer.class);
        int count = (maxSeries != null) ? maxSeries : 0;
        series.set(count + 1);
        String ticketNo = "SCL-" + Year.now().getValue() + "-" + series.getAndIncrement();

        jdbc.update(
                "INSERT INTO tickets (ticket_no, title, description, category, priority, " +
                        "status, reported_by, asset_id, department, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, 'Open', ?, ?, ?, " + IST_NOW + ")",
                ticketNo, title, description, category, priority, reportedBy, assetId, department);

        Integer newId = jdbc.queryForObject("SELECT MAX(id) FROM tickets", Integer.class);
        if (newId != null) {
            logStatusChange(newId, null, "Open", reportedBy, null);
        }
    }

    public int updateTicketStatus(int id, String status, String resolution, int changedBy) {
        Ticket before = getTicketById(id);
        int rows = jdbc.update(
                "UPDATE tickets SET " +
                        "status = ?, " +
                        "resolution = ?, " +
                        "updated_at = " + IST_NOW + ", " +
                        "resolved_at = CASE WHEN ? = 'Resolved' " +
                        "THEN " + IST_NOW + " ELSE resolved_at END, " +
                        "closure_requested_at = CASE WHEN ? = 'Resolved' " +
                        "THEN " + IST_NOW + " " +
                        "WHEN ? = 'In Progress' THEN NULL " +
                        "ELSE closure_requested_at END " +
                        "WHERE id = ?",
                status, resolution, status, status, status, id);

        if (rows > 0 && before != null) {
            String historyReason = ("Resolved".equals(status) || "In Progress".equals(status)) ? resolution : null;
            logStatusChange(id, before.status(), status, changedBy, historyReason);
        }
        return rows;
    }

    public int approveClosure(int id) {
        int rows = jdbc.update(
                "UPDATE tickets SET status = 'Closed', " +
                        "updated_at = " + IST_NOW + " WHERE id = ?", id);
        if (rows > 0) {
            logStatusChange(id, "Resolved", "Closed", 0, null);
        }
        return rows;
    }

    public int denyClosure(int id, String reason) {
        int rows = jdbc.update(
                "UPDATE tickets SET status = 'In Progress', " +
                        "closure_denial_reason = ?, " +
                        "closure_requested_at = NULL, " +
                        "updated_at = " + IST_NOW + " WHERE id = ?",
                reason, id);
        if (rows > 0) {
            logStatusChange(id, "Resolved", "In Progress", 0, reason);
        }
        return rows;
    }

    public List<Ticket> getTicketsPendingClosure(int employeeId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM tickets WHERE status = 'Resolved' " +
                        "AND reported_by = ? ORDER BY closure_requested_at ASC",
                employeeId);
        List<Ticket> tickets = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            tickets.add(mapRowToTicket(row));
        }
        return tickets;
    }

    public int assignTicket(int id, int userId) {
        int rows = jdbc.update(
                "UPDATE tickets SET assigned_to = ?, updated_at = " + IST_NOW + " WHERE id = ?",
                userId, id);
        return rows;
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
                        "VALUES (?, ?, ?, " + IST_NOW + ")",
                ticketId, comment, addedBy);
    }

    public List<Ticket> getTicketsByReporter(int employeeId) {
        List<Map<String,Object>> rows = jdbc.queryForList(
                "SELECT * FROM tickets WHERE reported_by = ? " +
                        "ORDER BY created_at DESC", employeeId);
        List<Ticket> tickets = new ArrayList<>();
        for (Map<String,Object> row : rows) {
            tickets.add(mapRowToTicket(row));
        }
        return tickets;
    }

    public List<Ticket> getTicketsByAsset(int assetId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM tickets WHERE asset_id = ? " +
                        "ORDER BY created_at DESC", assetId);
        List<Ticket> tickets = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            tickets.add(mapRowToTicket(row));
        }
        return tickets;
    }

    public List<Map<String, Object>> getStatusHistory(int ticketId) {
        return jdbc.queryForList(
                "SELECT h.*, u.full_name as changed_by_name " +
                        "FROM ticket_status_history h " +
                        "LEFT JOIN users u ON h.changed_by = u.id " +
                        "WHERE h.ticket_id = ? ORDER BY h.id ASC", ticketId);
    }

    /** Shapes a flat history list into up to 5 Resolved→(Closed|In Progress) cycles for reporting. */
    public List<Map<String, String>> getShapedCycles(int ticketId) {
        List<Map<String, Object>> history = getStatusHistory(ticketId);
        List<Map<String, String>> cycles = new ArrayList<>();
        String pendingResolvedAt = null;
        String pendingResolutionText = null;
        for (Map<String, Object> row : history) {
            String toStatus = (String) row.get("to_status");
            String changedAt = (String) row.get("changed_at");
            String reason = (String) row.get("reason");
            if ("Resolved".equals(toStatus)) {
                pendingResolvedAt = changedAt;
                pendingResolutionText = reason;
            } else if (pendingResolvedAt != null
                    && ("Closed".equals(toStatus) || "In Progress".equals(toStatus))) {
                Map<String, String> cycle = new java.util.HashMap<>();
                cycle.put("resolvedAt", pendingResolvedAt);
                cycle.put("resolutionText", pendingResolutionText == null ? "" : pendingResolutionText);
                cycle.put("closedOrDeniedAt", changedAt);
                cycle.put("outcome", toStatus);
                cycle.put("reason", reason == null ? "" : reason); // deny reason — only meaningful when outcome is "In Progress"
                cycles.add(cycle);
                pendingResolvedAt = null;
                pendingResolutionText = null;
                if (cycles.size() >= 5) break;
            }
        }
        return cycles;
    }

    /** Tickets created within [fromDate, toDate] inclusive, by date only (yyyy-MM-dd). */
    public List<Ticket> getTicketsForExport(String fromDate, String toDate) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM tickets WHERE date(created_at) BETWEEN ? AND ? ORDER BY created_at ASC",
                fromDate, toDate);
        List<Ticket> tickets = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            tickets.add(mapRowToTicket(row));
        }
        return tickets;
    }
    private int safeAssetId(Object raw) {
        if (raw == null) return 0;
        if (raw instanceof Number) return ((Number) raw).intValue();
        try {
            return Integer.parseInt(raw.toString().trim());
        } catch (NumberFormatException e) {
            return 0; // legacy row still has the old asset *name* string here — treat as "no asset"
        }
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
                safeAssetId(row.get("asset_id")),
                row.get("assigned_to") != null
                        ? ((Number) row.get("assigned_to")).intValue() : 0,
                (String) row.get("created_at"),
                (String) row.get("updated_at"),
                (String) row.get("resolved_at"),
                (String) row.get("resolution"),
                (String) row.get("department"),
                (String) row.get("closure_denial_reason"),
                (String) row.get("closure_requested_at")
        );
    }
}