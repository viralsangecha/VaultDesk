package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.DashboardDAO;
import com.vaultdesk.server.model.DashboardStats;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardDAO dashboardDAO;

    public DashboardController(DashboardDAO dashboardDAO)
    {
        this.dashboardDAO=dashboardDAO;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getDashboardStats()
    {
        int assets    = dashboardDAO.getTotalAssets();
        int tickets   = dashboardDAO.getOpenTicketsCount();
        int licenses  = dashboardDAO.getExpiringLicensesCount();
        int employees = dashboardDAO.getTotalEmployees();

        return ResponseEntity.ok(new DashboardStats(assets, tickets, licenses, employees));
    }

    @GetMapping("/recent-activity")
    public ResponseEntity<?> getrecentactivity()
    {
        return ResponseEntity.ok(dashboardDAO.getRecentTickets());
    }
}
