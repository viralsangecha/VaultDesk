package com.vaultdesk.server.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/backup")
public class BackupController {

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadBackup() {
        try {
            File db = new File("vaultdesk.db");
            if (!db.exists())
                return ResponseEntity.notFound().build();

            // ── Create timestamped copy ───────────────────
            String timestamp = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            File backup = new File(
                    "vaultdesk_backup_" + timestamp + ".db");
            Files.copy(db.toPath(), backup.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);

            Resource resource = new FileSystemResource(backup);
            return ResponseEntity.ok()
                    .header("Content-Disposition",
                            "attachment; filename="
                                    + backup.getName())
                    .header("Content-Type",
                            "application/octet-stream")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/info")
    public ResponseEntity<?> getBackupInfo() {
        File db = new File("vaultdesk.db");
        if (!db.exists())
            return ResponseEntity.notFound().build();

        long sizeKb = db.length() / 1024;
        String lastModified = LocalDateTime
                .ofInstant(
                        java.time.Instant.ofEpochMilli(
                                db.lastModified()),
                        java.time.ZoneId.systemDefault())
                .format(DateTimeFormatter
                        .ofPattern("yyyy-MM-dd HH:mm:ss"));

        return ResponseEntity.ok(java.util.Map.of(
                "sizeKb",       sizeKb,
                "lastModified", lastModified,
                "filename",     db.getName()
        ));
    }
}