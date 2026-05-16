package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.NotificationDAO;
import com.vaultdesk.server.dao.TicketDAO;
import com.vaultdesk.server.dao.UserDAO;
import com.vaultdesk.server.model.Ticket;
import com.vaultdesk.server.model.TicketRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketDAO ticketDAO;
    private final NotificationDAO notificationDAO;
    private final UserDAO userDAO;
    private final JdbcTemplate jdbc;

    public TicketController(TicketDAO ticketDAO,
                            NotificationDAO notificationDAO,
                            UserDAO userDAO,
                            JdbcTemplate jdbc) {
        this.ticketDAO        = ticketDAO;
        this.notificationDAO  = notificationDAO;
        this.userDAO          = userDAO;
        this.jdbc             = jdbc;
    }

    @GetMapping
    public ResponseEntity<?> getalltickets() {
        return ResponseEntity.ok(ticketDAO.getAllTickets());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getticketbyid(@PathVariable int id) {
        Ticket ticket = ticketDAO.getTicketById(id);
        if (ticket == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/open")
    public ResponseEntity<?> getticketbystatus() {
        List<Ticket> ticket = ticketDAO.getOpenTickets();
        if (ticket == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/assignee/{userId}")
    public ResponseEntity<?> getticketbyassign(@PathVariable int userId) {
        List<Ticket> ticket = ticketDAO.getTicketsByAssignee(userId);
        if (ticket == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ticket);
    }

    @PostMapping
    public ResponseEntity<?> savetickets(@RequestBody TicketRequest request) {
        ticketDAO.saveTicket(request.title(), request.description(),
                request.category(), request.priority(), request.reportedBy());

        // ── Get the new ticket id ─────────────────────────
        List<Ticket> all = ticketDAO.getAllTickets();
        int newTicketId = all != null && !all.isEmpty()
                ? all.get(0).id() : 0;

        // ── Notify all admins ─────────────────────────────
        notificationDAO.notifyAllAdmins(jdbc,
                "New ticket raised: " + request.title(),
                "TICKET_CREATED", newTicketId);

        return ResponseEntity.status(201).body("Ticket added");
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateticketstatus(
            @PathVariable int id,
            @RequestParam String status,
            @RequestParam String resolution) {
        int rows = ticketDAO.updateTicketStatus(id, status, resolution);
        if (rows == 0) return ResponseEntity.notFound().build();

        // ── Notify employee who raised it ─────────────────
        Ticket ticket = ticketDAO.getTicketById(id);
        if (ticket != null && ticket.reportedBy() > 0) {
            notificationDAO.notifyEmployee(
                    ticket.reportedBy(),
                    "Your ticket '" + ticket.title()
                            + "' status changed to: " + status,
                    "STATUS_CHANGED", id);
        }

        return ResponseEntity.ok("Ticket Status Updated!");
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<?> assignticket(
            @PathVariable int id,
            @RequestParam int userId) {
        int rows = ticketDAO.assignTicket(id, userId);
        if (rows == 0) return ResponseEntity.notFound().build();

        // ── Notify the assigned engineer/admin ────────────
        Ticket ticket = ticketDAO.getTicketById(id);
        if (ticket != null) {
            notificationDAO.notifyUser(userId,
                    "Ticket assigned to you: " + ticket.title(),
                    "TICKET_ASSIGNED", id);
        }

        return ResponseEntity.ok("Ticket assigned");
    }

    @GetMapping("/department/{deptId}")
    public ResponseEntity<?> getByDept(@PathVariable int deptId) {
        return ResponseEntity.ok(ticketDAO.getTicketsByDept(deptId));
    }

    // ── Comments ──────────────────────────────────────────
    @GetMapping("/{id}/comments")
    public ResponseEntity<?> getComments(@PathVariable int id) {
        return ResponseEntity.ok(ticketDAO.getComments(id));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<?> addComment(
            @PathVariable int id,
            @RequestBody Map<String, String> body) {
        String comment = body.get("comment");
        int addedBy = body.get("addedBy") != null
                ? Integer.parseInt(body.get("addedBy")) : 0;
        if (comment == null || comment.trim().isEmpty())
            return ResponseEntity.badRequest().body("Comment required");

        ticketDAO.saveComment(id, comment, addedBy);

        // ── Notify relevant party ─────────────────────────
        Ticket ticket = ticketDAO.getTicketById(id);
        if (ticket != null) {
            // If commented by employee → notify assigned user
            if (ticket.assignedTo() > 0) {
                notificationDAO.notifyUser(ticket.assignedTo(),
                        "New comment on ticket: " + ticket.title(),
                        "COMMENT_ADDED", id);
            }
            // Also notify employee if comment from admin/engineer
            if (ticket.reportedBy() > 0 && addedBy != ticket.reportedBy()) {
                notificationDAO.notifyEmployee(ticket.reportedBy(),
                        "New comment on your ticket: " + ticket.title(),
                        "COMMENT_ADDED", id);
            }
        }

        return ResponseEntity.status(201).body("Comment added");
    }
}