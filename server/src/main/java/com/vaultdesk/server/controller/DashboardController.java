package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.DashboardDAO;
import com.vaultdesk.server.model.DashboardStats;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardDAO dashboardDAO;

    public DashboardController(DashboardDAO dashboardDAO) {
        this.dashboardDAO = dashboardDAO;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getDashboardStats() {
        return ResponseEntity.ok(new DashboardStats(
                dashboardDAO.getTotalAssets(),
                dashboardDAO.getOpenTicketsCount(),
                dashboardDAO.getGeneralTicketCount(),
                dashboardDAO.getSapTicketCount(),
                dashboardDAO.getExpiringLicensesCount(),
                dashboardDAO.getTotalEmployees(),
                dashboardDAO.getTotalDepartments()
        ));
    }

    @GetMapping("/recent-activity")
    public ResponseEntity<?> getRecentActivity() {
        return ResponseEntity.ok(dashboardDAO.getRecentTickets());
    }
}