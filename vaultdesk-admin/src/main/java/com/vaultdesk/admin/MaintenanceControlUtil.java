package com.vaultdesk.admin;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MaintenanceControlUtil {

    // ⚠️ Adjust these three to match your real setup.
    private static final String WINDOWS_SERVICE_NAME = "VaultDeskServer";
    private static final String MAINTENANCE_JAR_PATH = "C:\\VaultDesk-Server\\MaintenanceServer.jar";
    private static final int SERVER_PORT = 2008;

    private static Process maintenanceProcess = null;

    public static void startMaintenance(Runnable onSuccess, java.util.function.Consumer<String> onError) {
        Thread worker = new Thread(() -> {
            try {
                File jar = new File(MAINTENANCE_JAR_PATH);
                if (!jar.exists()) {
                    onError.accept("Maintenance jar not found at: " + MAINTENANCE_JAR_PATH);
                    return;
                }
                if (!serviceExists(WINDOWS_SERVICE_NAME)) {
                    onError.accept("Windows service '" + WINDOWS_SERVICE_NAME + "' was not found.");
                    return;
                }

                ProcessBuilder placeholderPb = new ProcessBuilder(
                        "java", "-jar", MAINTENANCE_JAR_PATH, String.valueOf(SERVER_PORT));
                placeholderPb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                placeholderPb.redirectErrorStream(true);
                maintenanceProcess = placeholderPb.start();

                // Give it a moment, then actually verify it's still alive before touching the real service.
                Thread.sleep(1500);
                if (!maintenanceProcess.isAlive()) {
                    String err = new String(maintenanceProcess.getInputStream().readAllBytes());
                    onError.accept("Maintenance placeholder failed to start (exit code "
                            + maintenanceProcess.exitValue() + ").\nOutput:\n" + err.trim()
                            + "\n\nReal service was NOT stopped, since nothing is ready to take its place.");
                    return;
                }

                Process stopProc = new ProcessBuilder("net", "stop", WINDOWS_SERVICE_NAME)
                        .redirectErrorStream(true).start();
                String stopOutput = readOutput(stopProc);
                boolean stopped = stopProc.waitFor(20, TimeUnit.SECONDS) && stopProc.exitValue() == 0;

                if (!stopped) {
                    maintenanceProcess.destroy();
                    onError.accept("Could not stop the Windows service.\n\n" + stopOutput.trim());
                    return;
                }

                onSuccess.run();
            } catch (Exception ex) {
                onError.accept(ex.getMessage());
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    public static void resumeServer(Runnable onSuccess, java.util.function.Consumer<String> onError) {
        Thread worker = new Thread(() -> {
            try {
                if (maintenanceProcess != null && maintenanceProcess.isAlive()) {
                    maintenanceProcess.destroy();
                    maintenanceProcess.waitFor(5, TimeUnit.SECONDS); // actually wait for the port to free up
                } else {
                    killProcessOnPort(SERVER_PORT);
                    Thread.sleep(1500); // give Windows a moment to release the port after Stop-Process
                }

                Process startProc = new ProcessBuilder("net", "start", WINDOWS_SERVICE_NAME)
                        .redirectErrorStream(true).start();
                String startOutput = readOutput(startProc);
                boolean started = startProc.waitFor(20, TimeUnit.SECONDS) && startProc.exitValue() == 0;

                if (!started) {
                    if (startOutput.toLowerCase().contains("already")) {
                        onSuccess.run(); // it was already running from a prior click — treat as success, not an error
                        return;
                    }
                    onError.accept("Could not restart the Windows service.\n\n" + startOutput.trim());
                    return;
                }
                onSuccess.run();
            } catch (Exception ex) {
                onError.accept(ex.getMessage());
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private static boolean serviceExists(String name) {
        try {
            Process query = new ProcessBuilder("sc", "query", name)
                    .redirectErrorStream(true).start();
            String output = readOutput(query);
            query.waitFor(5, TimeUnit.SECONDS);
            return !output.contains("FAILED 1060"); // 1060 = "service does not exist" in Windows
        } catch (Exception e) {
            return false;
        }
    }

    private static String readOutput(Process p) throws IOException {
        return new String(p.getInputStream().readAllBytes());
    }

    private static void killProcessOnPort(int port) {
        try {
            Process p = new ProcessBuilder(
                    "powershell", "-NoProfile", "-Command",
                    "Get-NetTCPConnection -LocalPort " + port + " -State Listen -ErrorAction SilentlyContinue " +
                            "| Select-Object -ExpandProperty OwningProcess " +
                            "| ForEach-Object { Stop-Process -Id $_ -Force }"
            ).redirectErrorStream(true).start();
            p.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception ignored) {}
    }
}