package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.*;
import com.vaultdesk.server.model.Asset;
import com.vaultdesk.server.model.Ticket;
import com.vaultdesk.server.model.TicketRequest;
import com.vaultdesk.server.security.AuthContext;
import com.vaultdesk.server.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketDAO ticketDAO;
    private final NotificationDAO notificationDAO;
    private final UserDAO userDAO;
    private final JdbcTemplate jdbc;
    private final EmailService emailService;
    private final EmployeeDAO employeeDAO;
    private final AssetDAO assetDAO;
    private final AssetLinkDAO assetLinkDAO;

    public TicketController(TicketDAO ticketDAO,
                            NotificationDAO notificationDAO,
                            UserDAO userDAO,
                            JdbcTemplate jdbc, EmailService emailService, EmployeeDAO employeeDAO, AssetDAO assetDAO, AssetLinkDAO assetLinkDAO) {
        this.ticketDAO        = ticketDAO;
        this.notificationDAO  = notificationDAO;
        this.userDAO          = userDAO;
        this.jdbc             = jdbc;
        this.emailService = emailService;
        this.employeeDAO = employeeDAO;
        this.assetDAO=assetDAO;
        this.assetLinkDAO = assetLinkDAO;
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

    @GetMapping("/assignee/{userId}/department/{seldeptId}")
    public ResponseEntity<?> getticketbyassign(@PathVariable int userId,@PathVariable int seldeptId) {
        List<Ticket> ticket = ticketDAO.getTicketsByAssigneeByDept(userId,seldeptId);
        if (ticket == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/api/employees/{id}/ticket-defaults")
    public ResponseEntity<?> getTicketDefaults(@PathVariable int id) {
        Map<String, Object> result = new HashMap<>();
        result.put("department", employeeDAO.getDepartmentNameForEmployee(id));

        List<Asset> assets = assetDAO.getAssetsByEmployee(id);
        List<Integer> ids = assets.stream().map(Asset::id).toList();
        List<Map<String, Object>> links = assetLinkDAO.getLinksForAssetIds(ids);

        List<Map<String, Object>> assetList = new ArrayList<>();
        for (Asset a : assets) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.id());
            m.put("assetTag", a.assetTag());
            m.put("name", a.name());
            long accCount = links.stream()
                    .filter(l -> ((Number) l.get("owner_asset_id")).intValue() == a.id())
                    .count();
            m.put("accessoryCount", accCount);
            assetList.add(m);
        }
        result.put("assets", assetList);
        return ResponseEntity.ok(result);
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

    private void logActivity(int userId, String action,
                             String tableName, int recordId,
                             String details) {
        try {
            jdbc.update(
                    "INSERT INTO activity_log " +
                            "(user_id, action, table_name, " +
                            "record_id, details, logged_at) " +
                            "VALUES (?, ?, ?, ?, ?, datetime('now'))",
                    userId, action, tableName, recordId, details);
        } catch (Exception e) {
            System.out.println("Activity log error: " + e.getMessage());
        }
    }
    @PostMapping
    public ResponseEntity<?> savetickets(@RequestBody TicketRequest request) {
        String department = employeeDAO.getDepartmentNameForEmployee(request.reportedBy());
        ticketDAO.saveTicket(request.title(), request.description(),
                request.category(), request.priority(),
                request.reportedBy(), department, request.assetId());

        Integer newId = null;
        List<Ticket> recent = ticketDAO.getAllTickets();
        String ticketNo = "";
        for (Ticket t : recent) {
            if (t.reportedBy() == request.reportedBy()
                    && t.title().equals(request.title())) {
                newId = t.id();
                ticketNo = t.ticketNo();
                break;
            }
        }

        var employee = employeeDAO.getEmployeeById(request.reportedBy());
        var user = userDAO.getUserById(request.reportedBy());
        String reporterName = employee != null
                ? employee.name()
                : user != null
                ? user.fullName()
                : "Unknown";
        String reporterEmail = employee != null ? employee.email() : (user != null ? user.email() : null);

        emailService.sendTicketCreatedEmail(reporterEmail, ticketNo, request.title(), request.description(), reporterName);

        return ResponseEntity.status(201).body(Map.of(
                "success", true,
                "message", "Ticket created",
                "id", newId != null ? newId : 0,
                "ticketNo", ticketNo
        ));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateticketstatus(
            @PathVariable int id,
            @RequestParam String status,
            @RequestParam(required = false, defaultValue = "") String resolution) {

        Ticket ticket = ticketDAO.getTicketById(id);
        if (ticket == null) return ResponseEntity.notFound().build();

        if (ticket.assignedTo() <= 0) {
            return ResponseEntity.badRequest()
                    .body("Ticket must be assigned to an engineer before its status can be changed.");
        }

        int changedBy = com.vaultdesk.server.security.AuthContext.currentSubjectId();
        int rows = ticketDAO.updateTicketStatus(id, status, resolution, changedBy);
        if (rows == 0) return ResponseEntity.notFound().build();

        if (ticket.reportedBy() > 0) {
            notificationDAO.notifyEmployee(ticket.reportedBy(),
                    "Your ticket status changed to: " + status,
                    "STATUS_CHANGED", id);
        }

        var employee = employeeDAO.getEmployeeById(ticket.reportedBy());
        var user = userDAO.getUserById(ticket.reportedBy());
        String reporterName = employee != null
                ? employee.name()
                : user != null
                ? user.fullName()
                : "Unknown";
        String reporteremail = employee != null ? employee.email() : (user != null ? user.email() : null);

        String assignedTo = ticket.assignedTo() > 0
                ? userDAO.getUserById(ticket.assignedTo()).fullName()
                : "Unassigned";

        String ticketNo = ticket.ticketNo();

        String assigneeEmail = ticket.assignedTo() > 0 ? userDAO.getUserById(ticket.assignedTo()).email() : null;
        emailService.sendTicketStatusEmail(reporteremail, assigneeEmail, ticketNo, ticket.title(), reporterName, assignedTo, ticket.description(), status, resolution);

        return ResponseEntity.ok("Status updated");
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<?> getHistory(@PathVariable int id) {
        Map<String, Object> result = new HashMap<>();
        result.put("timeline", ticketDAO.getStatusHistory(id));
        result.put("cycles", ticketDAO.getShapedCycles(id));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/export")
    public ResponseEntity<?> exportTickets(@RequestParam String from, @RequestParam String to) {
        List<Ticket> tickets = ticketDAO.getTicketsForExport(from, to);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Ticket t : tickets) {
            Map<String, Object> row = new HashMap<>();
            row.put("ticket", t);
            row.put("cycles", ticketDAO.getShapedCycles(t.id()));
            result.add(row);
        }
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}/approve-closure")
    public ResponseEntity<?> approveClosure(@PathVariable int id) {
        int rows = ticketDAO.approveClosure(id);
        if (rows == 0) return ResponseEntity.notFound().build();

        Ticket ticket = ticketDAO.getTicketById(id);
        if (ticket == null) return ResponseEntity.notFound().build();

        var employee = employeeDAO.getEmployeeById(ticket.reportedBy());
        var user = userDAO.getUserById(ticket.reportedBy());
        String reporterName = employee != null
                ? employee.name()
                : user != null
                ? user.fullName()
                : "Unknown";
        String assignedTo = ticket.assignedTo() > 0
                ? userDAO.getUserById(ticket.assignedTo()).fullName()
                : "Unassigned";
        String ticketNo = ticket.ticketNo();

        String reporteremail = employee != null ? employee.email() : (user != null ? user.email() : null);
        String assigneeEmail = ticket.assignedTo() > 0 ? userDAO.getUserById(ticket.assignedTo()).email() : null;
        emailService.sendTicketStatusEmail(reporteremail, assigneeEmail, ticketNo, ticket.title(), reporterName, assignedTo, ticket.description(), ticket.status(),ticket.resolution());

        return ResponseEntity.ok("Ticket closed");
    }

    @PutMapping("/{id}/deny-closure")
    public ResponseEntity<?> denyClosure(@PathVariable int id, @RequestBody Map<String, String> body) {
        String reason = body.get("reason");
        int rows = ticketDAO.denyClosure(id, reason);
        if (rows == 0) return ResponseEntity.notFound().build();

        Ticket ticket = ticketDAO.getTicketById(id);
        if (ticket == null) return ResponseEntity.notFound().build();

        if (ticket.assignedTo() > 0) {
            notificationDAO.notifyUser(ticket.assignedTo(),
                    "Ticket closure denied: " + ticket.title(),
                    "STATUS_CHANGED", id);
        }

        var employee = employeeDAO.getEmployeeById(ticket.reportedBy());
        var user = userDAO.getUserById(ticket.reportedBy());
        String reporterName = employee != null
                ? employee.name()
                : user != null
                ? user.fullName()
                : "Unknown";
        String assignedTo = ticket.assignedTo() > 0
                ? userDAO.getUserById(ticket.assignedTo()).fullName()
                : "Unassigned";
        String ticketNo = ticket.ticketNo();

        String reporteremail = employee != null ? employee.email() : (user != null ? user.email() : null);
        String assigneeEmail = ticket.assignedTo() > 0 ? userDAO.getUserById(ticket.assignedTo()).email() : null;
        emailService.sendTicketStatusEmail(reporteremail, assigneeEmail, ticketNo, ticket.title(), reporterName, assignedTo, ticket.description(), ticket.status(), ticket.resolution());
        return ResponseEntity.ok("Closure denied, ticket reopened");
    }

    @GetMapping("/pending-closure/reporter/{employeeId}")
    public ResponseEntity<?> getPendingClosure(@PathVariable int employeeId) {
        return ResponseEntity.ok(
                ticketDAO.getTicketsPendingClosure(employeeId));
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<?> assignticket(
            @PathVariable int id,
            @RequestParam int userId) {
        int rows = ticketDAO.assignTicket(id, userId);
        if (rows == 0) return ResponseEntity.notFound().build();

        Ticket ticket = ticketDAO.getTicketById(id);
        if (ticket != null) {
            notificationDAO.notifyUser(userId,
                    "Ticket assigned to you: " + ticket.title(),
                    "TICKET_ASSIGNED", id);
        }
        ticketDAO.updateTicketStatus(id, "In Progress", "", 0);

        Ticket all = ticketDAO.getTicketById(id);

        String ticketNo = all.ticketNo();
        var employee = employeeDAO.getEmployeeById(ticket.reportedBy());
        var user = userDAO.getUserById(ticket.reportedBy());
        String assignedTo = userDAO.getUserById(ticket.assignedTo()).fullName();

        String reporterName = employee != null
                ? employee.name()
                : user != null
                ? user.fullName()
                : "Unknown";
        String reporterEmail = employee != null ? employee.email() : (user != null ? user.email() : null);
        String assigneeEmail = ticket.assignedTo() > 0 ? userDAO.getUserById(ticket.assignedTo()).email() : null;

        emailService.sendTicketStatusEmail(reporterEmail, assigneeEmail, ticketNo, ticket.title(), reporterName, assignedTo, ticket.description(), ticket.status(),ticket.resolution() );
        return ResponseEntity.ok("Ticket assigned");
    }

    @GetMapping("/asset/{assetId}")
    public ResponseEntity<?> getByAsset(
            @PathVariable int assetId) {
        return ResponseEntity.ok(
                ticketDAO.getTicketsByAsset(assetId));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<?> addComment(
            @PathVariable int id,
            @RequestBody Map<String, String> body) {
        String comment = body.get("comment");
        int addedBy = body.get("addedBy") != null
                ? Integer.parseInt(body.get("addedBy")) : 0;
        if (comment == null || comment.trim().isEmpty())
            return ResponseEntity.badRequest()
                    .body("Comment required");

        ticketDAO.saveComment(id, comment, addedBy);

        Ticket ticket = ticketDAO.getTicketById(id);
        if (ticket != null) {
            if (ticket.assignedTo() > 0) {
                notificationDAO.notifyUser(ticket.assignedTo(),
                        "New comment on ticket: " + ticket.title(),
                        "COMMENT_ADDED", id);
            }
            if (ticket.reportedBy() > 0
                    && addedBy != ticket.reportedBy()) {
                notificationDAO.notifyEmployee(ticket.reportedBy(),
                        "New comment on your ticket: " + ticket.title(),
                        "COMMENT_ADDED", id);
            }
        }

        return ResponseEntity.status(201).body("Comment added");
    }
}