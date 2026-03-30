package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.AssetDAO;
import com.vaultdesk.server.dao.LicenseDAO;
import com.vaultdesk.server.dao.TicketDAO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final LicenseDAO licenseDAO;
    private  final AssetDAO assetDAO;
    private  final TicketDAO ticketDAO;

    public ReportController(AssetDAO assetDAO, LicenseDAO licenseDAO, TicketDAO ticketDAO) {
        this.assetDAO = assetDAO;
        this.licenseDAO = licenseDAO;
        this.ticketDAO = ticketDAO; }

    @GetMapping("/assets")
    public ResponseEntity<?> getallasset() {
        return ResponseEntity.ok(assetDAO.getAllAssets());
    }

    @GetMapping("/tickets")
    public ResponseEntity<?> getalltickets()
    {
        return ResponseEntity.ok(ticketDAO.getAllTickets());
    }

    @GetMapping("/licenses")
    public ResponseEntity<?> getalllicence()
    {
        return ResponseEntity.ok(licenseDAO.getAllLicenses());
    }
}
