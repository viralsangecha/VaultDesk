package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.TicketDAO;
import com.vaultdesk.server.model.Ticket;
import com.vaultdesk.server.model.TicketComment;
import com.vaultdesk.server.model.TicketRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketDAO ticketDAO;

    public TicketController(TicketDAO ticketDAO) {
        this.ticketDAO = ticketDAO;
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
        return ResponseEntity.status(201).body("Ticket added");
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateticketstatus(@PathVariable int id,
                                                @RequestParam String status,
                                                @RequestParam String resolution) {
        int rows = ticketDAO.updateTicketStatus(id, status, resolution);
        if (rows == 0) return ResponseEntity.notFound().build();
        return ResponseEntity.ok("Ticket Status Updated!");
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<?> assignticket(@PathVariable int id,
                                          @RequestParam int userId) {
        int rows = ticketDAO.assignTicket(id, userId);
        if (rows == 0) return ResponseEntity.notFound().build();
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
    public ResponseEntity<?> addComment(@PathVariable int id,
                                        @RequestBody Map<String, String> body) {
        String comment = body.get("comment");
        int addedBy = body.get("addedBy") != null
                ? Integer.parseInt(body.get("addedBy")) : 0;
        if (comment == null || comment.trim().isEmpty())
            return ResponseEntity.badRequest().body("Comment is required");
        ticketDAO.saveComment(id, comment, addedBy);
        return ResponseEntity.status(201).body("Comment added");
    }
}