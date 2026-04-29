package com.vaultdesk.admin;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class CsvImporter {

    public interface RowHandler {
        void handle(String[] fields) throws Exception;
    }

    // ── Generic CSV import ────────────────────────────────
    // Returns number of rows imported, -1 on cancel
    public static int importCsv(String title,
                                boolean skipHeader,
                                RowHandler handler) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = chooser.showOpenDialog(new Stage());
        if (file == null) return -1;

        int count = 0;
        int errors = 0;
        List<String> errorLines = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(
                new FileReader(file))) {
            String line;
            int lineNum = 0;
            while ((line = br.readLine()) != null) {
                lineNum++;
                if (lineNum == 1 && skipHeader) continue;
                if (line.trim().isEmpty()) continue;
                String[] fields = line.split(",", -1);
                // trim quotes and whitespace from each field
                for (int i = 0; i < fields.length; i++) {
                    fields[i] = fields[i].trim()
                            .replaceAll("^\"|\"$", "").trim();
                }
                try {
                    handler.handle(fields);
                    count++;
                } catch (Exception ex) {
                    errors++;
                    errorLines.add("Line " + lineNum + ": " + ex.getMessage());
                }
            }
        } catch (Exception ex) {
            showAlert("Import Error", "Could not read file: " + ex.getMessage());
            return -1;
        }

        String summary = "Imported: " + count + " rows.";
        if (errors > 0) {
            summary += "\nFailed: " + errors + " rows.";
            if (!errorLines.isEmpty()) {
                summary += "\n\nFirst error:\n" + errorLines.get(0);
            }
        }
        showAlert("Import Complete", summary);
        return count;
    }

    private static void showAlert(String title, String msg) {
        javafx.scene.control.Alert a =
                new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}