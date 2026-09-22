package com.vaultdesk.admin;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CsvImporter {

    public interface RowHandler {
        void handle(String[] fields) throws Exception;
    }

    public static class ImportResult {
        public final int total;
        public final int succeeded;
        public final List<String> failures; // "Row 4: <reason>"
        public ImportResult(int total, int succeeded, List<String> failures) {
            this.total = total; this.succeeded = succeeded; this.failures = failures;
        }
    }

    /** Async version — file dialog runs on the calling (FX) thread, then processing runs in the background with live progress. */
    public static void importCsvAsync(String title, boolean skipHeader,
                                      RowHandler handler, Consumer<ImportResult> onComplete) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = chooser.showOpenDialog(new Stage());
        if (file == null) return;

        List<String[]> allRows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNum = 0;
            while ((line = br.readLine()) != null) {
                lineNum++;
                if (lineNum == 1 && skipHeader) continue;
                if (line.trim().isEmpty()) continue;
                String[] fields = line.split(",", -1);
                for (int i = 0; i < fields.length; i++) {
                    fields[i] = fields[i].trim().replaceAll("^\"|\"$", "").trim();
                }
                allRows.add(fields);
            }
        } catch (Exception ex) {
            showAlert("Import Error", "Could not read file: " + ex.getMessage());
            return;
        }

        if (allRows.isEmpty()) {
            showAlert("Import", "No data rows found in the file.");
            return;
        }

        ProgressDialogUtil.Handle progress = ProgressDialogUtil.show("Importing " + title);

        Task<ImportResult> task = new Task<>() {
            @Override
            protected ImportResult call() {
                int succeeded = 0;
                List<String> failures = new ArrayList<>();
                int total = allRows.size();
                for (int i = 0; i < total; i++) {
                    String[] fields = allRows.get(i);
                    int rowNum = i + (skipHeader ? 2 : 1); // human-friendly line number, accounting for header
                    try {
                        handler.handle(fields);
                        succeeded++;
                    } catch (Exception ex) {
                        failures.add("Row " + rowNum + ": " + ex.getMessage());
                    }
                    progress.update(i + 1, total, "Processing row " + (i + 1) + " of " + total);
                }
                return new ImportResult(total, succeeded, failures);
            }
        };

        task.setOnSucceeded(e -> {
            progress.close();
            ImportResult result = task.getValue();
            showResultSummary(result);
            if (onComplete != null) Platform.runLater(() -> onComplete.accept(result));
        });
        task.setOnFailed(e -> {
            progress.close();
            showAlert("Import Failed", "Unexpected error: " + task.getException().getMessage());
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private static void showResultSummary(ImportResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Imported: ").append(result.succeeded).append(" / ").append(result.total).append(" rows.");
        if (!result.failures.isEmpty()) {
            sb.append("\nFailed: ").append(result.failures.size()).append(" rows.\n\n");
            int shown = Math.min(result.failures.size(), 15);
            for (int i = 0; i < shown; i++) sb.append(result.failures.get(i)).append("\n");
            if (result.failures.size() > shown) {
                sb.append("... and ").append(result.failures.size() - shown).append(" more.");
            }
        }
        javafx.scene.control.Alert a = new javafx.scene.control.Alert(
                result.failures.isEmpty() ? javafx.scene.control.Alert.AlertType.INFORMATION
                        : javafx.scene.control.Alert.AlertType.WARNING);
        a.setTitle("Import Complete");
        a.setHeaderText(null);
        a.getDialogPane().setPrefWidth(480);
        a.setContentText(sb.toString());
        a.showAndWait();
    }

    private static void showAlert(String title, String msg) {
        javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}