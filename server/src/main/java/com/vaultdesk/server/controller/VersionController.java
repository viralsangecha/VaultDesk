package com.vaultdesk.server.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/version")
public class VersionController {

    // ── Update this when you release a new version ────────
    private static final String LATEST_VERSION  = "1.0.0";
    private static final String DOWNLOAD_URL    =
            "http://localhost:8080/api/version/download";
    private static final String CHANGELOG       =
            "Initial release of VaultDesk Admin v1.0.0";

    @GetMapping
    public ResponseEntity<?> getVersion() {
        return ResponseEntity.ok(Map.of(
                "version",     LATEST_VERSION,
                "downloadUrl", DOWNLOAD_URL,
                "changelog",   CHANGELOG
        ));
    }
}