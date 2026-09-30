package com.vaultdesk.server.service;

import com.vaultdesk.server.dao.EmailRecipientDAO;
import com.vaultdesk.server.dao.EmailSettingsDAO;
import com.vaultdesk.server.dao.NotificationRuleDAO;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Properties;

@Service
public class EmailService {

    private static final String BLUE   = "#2980b9";
    private static final String GREEN  = "#27ae60";
    private static final String AMBER  = "#e67e22";
    private static final String RED    = "#c0392b";
    private static final String GREY   = "#6b7d8c";
    private static final String INK    = "#1e2b38";
    private static final String FAINT  = "#8c97a6";

    private final EmailSettingsDAO settingsDAO;
    private final EmailRecipientDAO recipientDAO;
    private final NotificationRuleDAO ruleDAO;

    public EmailService(EmailSettingsDAO settingsDAO, EmailRecipientDAO recipientDAO, NotificationRuleDAO ruleDAO) {
        this.settingsDAO = settingsDAO;
        this.recipientDAO = recipientDAO;
        this.ruleDAO = ruleDAO;
    }

    /** Every notifiable event goes through here — the DB rule decides who actually gets it. */
    private void notifyForEvent(String eventKey, String reporterEmail, String assigneeEmail, String subject, String htmlBody) {
        NotificationRuleDAO.Rule rule = ruleDAO.getRule(eventKey);
        if (rule == null || !rule.enabled()) return;

        java.util.Set<String> alreadySent = new java.util.HashSet<>();

        if (rule.notifyReporter() && reporterEmail != null && !reporterEmail.isBlank()) {
            send(reporterEmail, subject, htmlBody);
            alreadySent.add(reporterEmail.trim().toLowerCase());
        }
        if (rule.notifyAssignee() && assigneeEmail != null && !assigneeEmail.isBlank()
                && alreadySent.add(assigneeEmail.trim().toLowerCase())) {
            send(assigneeEmail, subject, htmlBody);
        }
        for (Map<String, Object> r : recipientDAO.getActiveSubscribers(eventKey)) {
            String email = (String) r.get("email");
            if (email == null || !alreadySent.add(email.trim().toLowerCase())) continue;
            send(email, subject, htmlBody);
        }
    }

    @Async
    public void sendTicketCreatedEmail(String reporterEmail, String ticketNo, String title, String description, String reporterName) {
        String subject = "[VaultDesk] Ticket Created - " + safe(ticketNo);
        String body = wrap("🎫", "New ticket created.",
                field("Ticket No", ticketNo) +
                        field("Title", title) +
                        field("Description", description) +
                        field("Reporter", reporterName) +
                        "<p style='color:" + GREY + ";font-size:12px;margin-top:12px;'>Log in to VaultDesk for updates.</p>",
                BLUE);
        notifyForEvent("TICKET_CREATED", reporterEmail, null, subject, body);
    }

    @Async
    public void sendTicketAssignedEmail(String reporterEmail, String assigneeEmail, String ticketNo, String title, String description,
                                        String reporterName, String engineerName, String newStatus) {
        String subject = "[VaultDesk] Ticket Assigned - " + safe(ticketNo);
        String body = wrap("👤", "Ticket has been assigned to an engineer.",
                field("Ticket No", ticketNo) +
                        field("Title", title) +
                        field("Description", description) +
                        field("Reporter", reporterName) +
                        field("Assigned To", engineerName),
                BLUE);
        notifyForEvent("TICKET_ASSIGNED", reporterEmail, assigneeEmail, subject, body);
    }

    @Async
    public void sendTicketStatusEmail(String reporterEmail, String assigneeEmail, String ticketNo, String title, String reporterName,
                                      String engineerName, String description, String newStatus, String resolution) {
        String safeStatus = safe(newStatus);
        String statusColor = switch (safeStatus) {
            case "Resolved" -> GREEN;
            case "In Progress" -> BLUE;
            case "Closed" -> GREY;
            default -> AMBER;
        };
        String subject = "[VaultDesk] Ticket Status Updated - " + safe(ticketNo);
        String resolutionHtml = (resolution != null && !resolution.isBlank())
                ? field("Resolution", resolution)
                : "";

        String body = wrap("🔄", "Your ticket status has been updated.",
                field("Ticket No", ticketNo) +
                        field("Title", title) +
                        field("Description", description) +
                        field("Reporter", reporterName) +
                        field("Assigned To", engineerName) +
                        resolutionHtml +
                        "<p style='margin-top:10px;'>" + chip(safeStatus, statusColor) + "</p>",
                statusColor);
        notifyForEvent("TICKET_STATUS_CHANGED", reporterEmail, assigneeEmail, subject, body);
    }

    @Async
    public void sendTicketAutoClosedToRecipients(String reporterEmail, String assigneeEmail, String ticketNo, String title, String reporterName) {
        String subject = "[VaultDesk] Ticket Auto-Closed - " + safe(ticketNo);
        String body = wrap("⏰", "A ticket was automatically closed after 3 days with no response.",
                field("Ticket No", ticketNo) +
                        field("Title", title) +
                        field("Reporter", reporterName) +
                        "<p style='margin-top:10px;'>" + chip("Auto-Closed", GREY) + "</p>",
                GREY);
        notifyForEvent("TICKET_AUTO_CLOSED", reporterEmail, assigneeEmail, subject, body);
    }

    private JavaMailSenderImpl buildSender(EmailSettingsDAO.EmailSettings s) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(s.smtpHost());
        sender.setPort(s.smtpPort());
        sender.setUsername(s.username());
        sender.setPassword(s.password());
        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", s.smtpHost());
        return sender;
    }

    private void send(String toEmail, String subject, String htmlBody) {
        EmailSettingsDAO.EmailSettings s = settingsDAO.getSettings();
        if (s == null || !s.enabled()) {
            System.out.println("Email not configured/enabled — skipped sending: " + subject);
            return;
        }
        try {
            JavaMailSenderImpl sender = buildSender(s);
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(s.username(), s.fromName() != null ? s.fromName() : "VaultDesk");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            sender.send(message);
        } catch (Exception e) {
            System.out.println("Email failed to " + toEmail + ": " + e.getMessage());
        }
    }

    /** One shared card shell. icon is a small emoji badge shown next to the app name for quick visual scanning in an inbox. */
    private String wrap(String icon, String headline, String innerHtml, String accentColor) {
        return "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' " +
                "style='background:#b8c6db;background:linear-gradient(135deg,#b8c6db,#a2b4cd);padding:32px 12px;font-family:Segoe UI,Arial,sans-serif;'>" +
                "<tr><td align='center'>" +
                "<table role='presentation' width='560' cellpadding='0' cellspacing='0' " +
                "style='max-width:560px;width:100%;background:#ffffff;border-radius:14px;overflow:hidden;box-shadow:0 4px 16px rgba(31,38,135,0.15);'>" +
                "<tr><td style='background:" + BLUE + ";background:linear-gradient(135deg,#2980b9,#3498db);padding:22px 28px;'>" +
                "<table role='presentation' cellpadding='0' cellspacing='0'><tr>" +
                "<td style='font-size:22px;padding-right:10px;vertical-align:middle;'>" + icon + "</td>" +
                "<td style='vertical-align:middle;'>" +
                "<span style='color:#ffffff;font-size:19px;font-weight:bold;'>VaultDesk</span>" +
                "<div style='color:#eaf3fb;font-size:11px;letter-spacing:1px;margin-top:2px;'>IT HELPDESK &amp; ASSET MANAGEMENT</div>" +
                "</td></tr></table>" +
                "</td></tr>" +
                "<tr><td style='padding:28px 28px 8px 28px;'>" +
                "<p style='color:" + INK + ";font-size:15px;font-weight:600;margin:0 0 16px 0;'>" + headline + "</p>" +
                "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' " +
                "style='background:#f4f6f9;border-left:4px solid " + accentColor + ";border-radius:8px;'>" +
                "<tr><td style='padding:16px 18px;color:" + INK + ";font-size:13px;line-height:1.7;'>" + innerHtml + "</td></tr>" +
                "</table>" +
                "</td></tr>" +
                "<tr><td style='padding:20px 28px 26px 28px;'>" +
                "<p style='color:" + FAINT + ";font-size:11px;text-align:center;margin:0;'>This is an automated message from VaultDesk. Please do not reply directly to this email.</p>" +
                "</td></tr>" +
                "</table>" +
                "</td></tr></table>";
    }

    private String safe(String value) {
        return (value == null || value.isBlank()) ? "—" : value.trim();
    }

    private String field(String label, String value) {
        return "<p style='margin:0 0 8px 0;'><span style='color:" + GREY + ";'>" + label + ":</span> "
                + "<b style='color:" + INK + ";'>" + safe(value) + "</b></p>";
    }

    private String chip(String text, String color) {
        return "<span style='display:inline-block;padding:2px 10px;border-radius:10px;font-size:11px;font-weight:bold;"
                + "color:#ffffff;background:" + color + ";'>" + text + "</span>";
    }

    // ── Forgot / reset password flow ──────────────────────
    @Async
    public void sendPasswordResetRequestedToAdmins(String username, String userType) {
        String subject = "[VaultDesk] Password Reset Requested - " + safe(username);
        String body = wrap("⚠️", "A password reset was requested.",
                field("Username", username) +
                        field("Account Type", userType) +
                        "<p style='color:" + GREY + ";font-size:12px;margin-top:10px;'>If this wasn't expected, please investigate.</p>",
                AMBER);
        notifyForEvent("PASSWORD_RESET_REQUESTED", null, null, subject, body);
    }

    @Async
    public void sendPasswordResetLink(String toEmail, String name, String resetLink) {
        String body = wrap("🔑", "You requested a password reset.",
                "<p style='margin:0 0 10px 0;'>Hi " + safe(name) + ",</p>" +
                        "<p style='margin:0 0 14px 0;'>Click the button below to reset your password. This link expires in 30 minutes.</p>" +
                        "<p style='text-align:center;margin:0 0 14px 0;'>" +
                        "<a href='" + resetLink + "' style='display:inline-block;padding:10px 24px;border-radius:8px;" +
                        "background:" + BLUE + ";color:#ffffff;font-weight:bold;text-decoration:none;'>Reset My Password</a></p>" +
                        "<p style='color:" + GREY + ";font-size:12px;'>If you didn't request this, you can safely ignore this email.</p>",
                BLUE);
        send(toEmail, "[VaultDesk] Reset Your Password", body);
    }

    @Async
    public void sendPasswordResetSuccessNotice(String toEmail, String username) {
        String body = wrap("✔️", "Password reset successful.",
                field("Username", username) +
                        "<p style='color:" + GREY + ";" +
                        "font-size:12px;margin-top:10px;'>This user's password was just reset successfully.</p>",
                GREEN);
        send(toEmail, "[VaultDesk] Password Reset Successful - " + safe(username), body);
    }

    @Async
    public void notifyAllAboutSuccessfulReset(String userEmail, String username) {
        String subject = "[VaultDesk] Password Reset Completed - " + safe(username);
        String body = wrap("✔️", "Password reset successful.",
                field("Username", username) +
                        "<p style='color:" + GREY + ";font-size:12px;margin-top:10px;'>This user's password was just reset successfully.</p>",
                GREEN);
        notifyForEvent("PASSWORD_RESET_COMPLETED", userEmail, null, subject, body);
    }
}