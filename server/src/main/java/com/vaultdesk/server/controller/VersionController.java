package com.vaultdesk.server.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.Map;

@RestController
@RequestMapping("/api/version")
public class VersionController {

    // ── Update these when releasing new version ───────────
    private static final String LATEST_VERSION = "1.0.0";
    private static final String CHANGELOG =
            "Initial release of VaultDesk Admin v1.0.0";

    @GetMapping
    public ResponseEntity<?> getVersion() {
        return ResponseEntity.ok(Map.of(
                "version",     LATEST_VERSION,
                "downloadUrl", "/api/version/download",
                "changelog",   CHANGELOG
        ));
    }

    @GetMapping("/download")
    public ResponseEntity<?> downloadJar() throws Exception {
        File jar = new File("vaultdesk-admin-fat.jar");
        if (!jar.exists())
            return ResponseEntity.notFound().build();

        byte[] bytes = java.nio.file.Files.readAllBytes(jar.toPath());
        return ResponseEntity.ok()
                .header("Content-Disposition",
                        "attachment; filename=vaultdesk-admin-fat.jar")
                .header("Content-Type", "application/octet-stream")
                .body(bytes);
    }
}