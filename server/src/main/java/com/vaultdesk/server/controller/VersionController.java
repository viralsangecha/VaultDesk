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
    private static final String LATEST_VERSION = "1.0.2";
    private static final String CHANGELOG =
            "Initial release of VaultDesk Admin v1.0.2";

    @GetMapping
    public ResponseEntity<?> getVersion() {
        return ResponseEntity.ok(Map.of(
                "version",     LATEST_VERSION,
                "downloadUrl", "/api/version/download",
                "changelog",   CHANGELOG
        ));
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadJar() {
        // Looks for JAR in same folder as server JAR
        File jar = new File("vaultdesk-admin-fat.jar");
        if (!jar.exists()) {
            System.out.println("Update JAR not found at: "
                    + jar.getAbsolutePath());
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(jar);
        return ResponseEntity.ok()
                .header("Content-Disposition",
                        "attachment; filename=vaultdesk-admin-fat.jar")
                .header("Content-Type", "application/java-archive")
                .body(resource);
    }
}