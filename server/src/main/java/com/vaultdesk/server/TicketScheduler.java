package com.vaultdesk.server;

import com.vaultdesk.server.dao.NotificationDAO;
import com.vaultdesk.server.dao.TicketDAO;
import com.vaultdesk.server.model.Ticket;
import com.vaultdesk.server.service.EmailService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TicketScheduler {

    private final JdbcTemplate jdbc;
    private final NotificationDAO notificationDAO;
    private final EmailService emailService;

    public TicketScheduler(JdbcTemplate jdbc,
                           NotificationDAO notificationDAO, EmailService emailService) {
        this.jdbc = jdbc;
        this.notificationDAO = notificationDAO;
        this.emailService = emailService;
    }

    // Runs every day at midnight
    @Scheduled(cron = "0 0 0 * * *")
    public void autoCloseResolvedTickets() {
        System.out.println("Running auto-close job...");

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT t.*, e.name as reporter_name, e.email as reporter_email, u.full_name as engineer_name, u.email as engineer_email " +
                        "FROM tickets t " +
                        "LEFT JOIN employees e ON e.id = t.reported_by " +
                        "LEFT JOIN users u ON u.id = t.assigned_to " +
                        "WHERE t.status = 'Resolved' " +
                        "AND t.closure_requested_at <= datetime('now','+5 hours','+30 minutes','-3 days')");

        for (Map<String, Object> row : rows) {
            int id = ((Number) row.get("id")).intValue();
            int reportedBy = row.get("reported_by") != null
                    ? ((Number) row.get("reported_by")).intValue() : 0;
            String title = (String) row.get("title");

            jdbc.update(
                    "UPDATE tickets SET status = 'Closed', " +
                            "updated_at = datetime('now','+5 hours','+30 minutes') WHERE id = ?", id);

            jdbc.update(
                    "INSERT INTO ticket_status_history (ticket_id, from_status, to_status, changed_by, changed_at, reason) " +
                            "VALUES (?, 'Resolved', 'Closed', 0, datetime('now','+5 hours','+30 minutes'), 'Auto-closed after 3 days')",
                    id);

            if (reportedBy > 0) {
                notificationDAO.notifyEmployee(reportedBy,
                        "Your ticket '" + title + "' was automatically closed after 3 days.",
                        "STATUS_CHANGED", id);
            }

            int assignedTo = row.get("assigned_to") != null
                    ? ((Number) row.get("assigned_to")).intValue() : 0;
            if (assignedTo > 0) {
                notificationDAO.notifyUser(assignedTo,
                        "Ticket '" + title + "' was automatically closed after 3 days.",
                        "STATUS_CHANGED", id);
            }

            String reporterEmail = (String) row.get("reporter_email");
            if (reporterEmail != null && !reporterEmail.isEmpty()) {
                String reporterName = row.get("reporter_name") != null ? (String) row.get("reporter_name") : "Employee";
                String ticketNo = (String) row.get("ticket_no");
                String assigneeEmail = (String) row.get("engineer_email");
                emailService.sendTicketAutoClosedToRecipients(reporterEmail, assigneeEmail, ticketNo, title, reporterName);
            }


            System.out.println("Auto-closed ticket #" + id);
        }
        System.out.println("Auto-close job done. Closed " + rows.size() + " tickets.");
    }
}