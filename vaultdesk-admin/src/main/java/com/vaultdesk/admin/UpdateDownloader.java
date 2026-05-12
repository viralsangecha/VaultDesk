package com.vaultdesk.admin;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;

public class UpdateDownloader {

    // ── Download new JAR to temp file ─────────────────────
    // ── Download new JAR to temp file ─────────────────────
    public static File download(String downloadUrl,
                                javafx.scene.control.ProgressBar progressBar)
            throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .GET().build();

        File tempFile = File.createTempFile("vaultdesk-update-", ".jar");
        // ── removed tempFile.deleteOnExit() — batch script needs it after System.exit() ──

        HttpResponse<InputStream> resp = client.send(req,
                HttpResponse.BodyHandlers.ofInputStream());

        if (resp.statusCode() != 200)
            throw new Exception("Download failed: HTTP " + resp.statusCode());

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

        String installDir    = System.getProperty("user.dir");
        String targetJarPath = installDir + "\\app\\vaultdesk-admin-fat.jar";
        String exePath       = installDir + "\\VaultDesk Admin.exe";
        String newJarPath    = newJar.getAbsolutePath();

        File batchFile = new File(System.getProperty("java.io.tmpdir"),
                "vaultdesk-update.bat");

        String script =
                "@echo off\r\n" +
                        "echo Applying VaultDesk update...\r\n" +
                        "timeout /t 3 /nobreak > nul\r\n" +
                        "copy /Y \"" + newJarPath + "\" \"" + targetJarPath + "\"\r\n" +
                        "if errorlevel 1 (\r\n" +
                        "    echo Copy failed! Path: " + targetJarPath + "\r\n" +
                        "    pause\r\n" +
                        "    exit /b 1\r\n" +
                        ")\r\n" +
                        "del \"" + newJarPath + "\"\r\n" +          // ← clean up temp JAR
                        "echo Update applied. Starting VaultDesk...\r\n" +
                        "start \"\" \"" + exePath + "\"\r\n" +
                        "exit\r\n";

        try (FileWriter fw = new FileWriter(batchFile)) {
            fw.write(script);
        }

        Runtime.getRuntime().exec(new String[]{
                "cmd.exe", "/c", "start", "VaultDesk Update",
                batchFile.getAbsolutePath()
        });

        System.exit(0);
    }
}