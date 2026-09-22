package com.vaultdesk.admin;

import javafx.application.Platform;
import javafx.concurrent.Task;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.function.Function;

public class ExcelExporter {

    /** Simple, fast path — use when rows are already fully built in memory (no per-row work needed). */
    public static void export(String sheetName, List<String> headers, List<List<String>> rows) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Excel Report");
        chooser.setInitialFileName(sheetName + ".xlsx");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        File file = chooser.showSaveDialog(new Stage());
        if (file == null) return;

        ProgressDialogUtil.Handle progress = ProgressDialogUtil.show("Exporting " + sheetName);
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                writeWorkbook(file, sheetName, headers, rows, progress);
                return null;
            }
        };
        task.setOnSucceeded(e -> { progress.close(); showSuccess("Exported to: " + file.getName()); });
        task.setOnFailed(e -> { progress.close(); showError("Export failed: " + task.getException().getMessage()); });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    /** Use when each row needs per-item work (e.g. resolving a name from a map) — still runs off the FX thread. */
    public static <T> void exportWithMapping(String sheetName, List<String> headers,
                                             List<T> items, Function<T, List<String>> rowMapper) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Excel Report");
        chooser.setInitialFileName(sheetName + ".xlsx");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        File file = chooser.showSaveDialog(new Stage());
        if (file == null) return;

        ProgressDialogUtil.Handle progress = ProgressDialogUtil.show("Exporting " + sheetName);
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                java.util.List<List<String>> rows = new java.util.ArrayList<>();
                int total = items.size();
                for (int i = 0; i < total; i++) {
                    rows.add(rowMapper.apply(items.get(i)));
                    progress.update(i + 1, total, "Preparing row " + (i + 1) + " of " + total);
                }
                writeWorkbook(file, sheetName, headers, rows, progress);
                return null;
            }
        };
        task.setOnSucceeded(e -> { progress.close(); showSuccess("Exported to: " + file.getName()); });
        task.setOnFailed(e -> { progress.close(); showError("Export failed: " + task.getException().getMessage()); });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private static void writeWorkbook(File file, String sheetName, List<String> headers,
                                      List<List<String>> rows, ProgressDialogUtil.Handle progress) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(sheetName);

            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);

            CellStyle altStyle = wb.createCellStyle();
            altStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            altStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            int total = rows.size();
            for (int r = 0; r < total; r++) {
                Row row = sheet.createRow(r + 1);
                List<String> rowData = rows.get(r);
                for (int c = 0; c < rowData.size(); c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(rowData.get(c));
                    if (r % 2 == 1) cell.setCellStyle(altStyle);
                }
                if (r % 20 == 0) progress.update(r + 1, total, "Writing row " + (r + 1) + " of " + total);
            }

            for (int i = 0; i < headers.size(); i++) sheet.autoSizeColumn(i);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }

    private static void showSuccess(String msg) {
        Platform.runLater(() -> {
            javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            a.setTitle("Export Successful"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
        });
    }

    private static void showError(String msg) {
        Platform.runLater(() -> {
            javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            a.setTitle("Export Failed"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
        });
    }
}