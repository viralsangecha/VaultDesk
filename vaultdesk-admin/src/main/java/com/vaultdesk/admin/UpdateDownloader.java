package com.vaultdesk.admin;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;

public class UpdateDownloader {

    // ── Download new JAR to temp file ─────────────────────
    public static File download(String downloadUrl,
                                javafx.scene.control.ProgressBar progressBar)
            throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .GET().build();

        // Download to temp file
        File tempFile = File.createTempFile("vaultdesk-update-", ".jar");
        tempFile.deleteOnExit();

        HttpResponse<InputStream> resp = client.send(req,
                HttpResponse.BodyHandlers.ofInputStream());

        if (resp.statusCode() != 200)
            throw new Exception("Download failed: HTTP " + resp.statusCode());

        // Get content length for progress if available
        long contentLength = resp.headers()
                .firstValueAsLong("content-length")
                .orElse(-1L);

        try (InputStream in = resp.body();
             FileOutputStream out = new FileOutputStream(tempFile)) {

            byte[] buffer = new byte[8192];
            long downloaded = 0;
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                downloaded += bytesRead;

                if (contentLength > 0 && progressBar != null) {
                    double progress = (double) downloaded / contentLength;
                    javafx.application.Platform.runLater(() ->
                            progressBar.setProgress(progress));
                }
            }
        }
        return tempFile;
    }

    // ── Write a batch script that replaces JAR + restarts ─
    public static void applyUpdate(File newJar) throws Exception {
        // Find current JAR path
        String currentJar = UpdateDownloader.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI()
                .getPath();

        // On Windows, path may start with /C:/ — fix it
        if (currentJar.startsWith("/") && currentJar.contains(":")) {
            currentJar = currentJar.substring(1);
        }

        String newJarPath = newJar.getAbsolutePath();

        // Write batch script
        File batchFile = new File(System.getProperty("java.io.tmpdir"),
                "vaultdesk-update.bat");

        String script =
                "@echo off\r\n" +
                        "echo Applying VaultDesk update...\r\n" +
                        "timeout /t 2 /nobreak > nul\r\n" +
                        // Wait for app to close, then replace JAR
                        "copy /Y \"" + newJarPath + "\" \""
                        + currentJar + "\"\r\n" +
                        "echo Starting VaultDesk...\r\n" +
                        "start \"\" javaw -jar \"" + currentJar + "\"\r\n" +
                        "del \"" + batchFile.getAbsolutePath() + "\"\r\n";

        try (FileWriter fw = new FileWriter(batchFile)) {
            fw.write(script);
        }

        // Run batch script and exit
        Runtime.getRuntime().exec(new String[]{
                "cmd.exe", "/c", "start", "",
                batchFile.getAbsolutePath()
        });

        System.exit(0);
    }
}