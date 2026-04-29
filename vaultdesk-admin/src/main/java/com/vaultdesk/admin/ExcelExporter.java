package com.vaultdesk.admin;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class ExcelExporter {

    // ── Generic export ────────────────────────────────────
    public static void export(String sheetName,
                              List<String> headers,
                              List<List<String>> rows) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Excel Report");
        chooser.setInitialFileName(sheetName + ".xlsx");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        File file = chooser.showSaveDialog(new Stage());
        if (file == null) return;

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(sheetName);

            // ── Header style ──────────────────────────────
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(
                    IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);

            // ── Alt row style ─────────────────────────────
            CellStyle altStyle = wb.createCellStyle();
            altStyle.setFillForegroundColor(
                    IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            altStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // ── Write headers ─────────────────────────────
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            // ── Write data rows ───────────────────────────
            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r + 1);
                List<String> rowData = rows.get(r);
                for (int c = 0; c < rowData.size(); c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(rowData.get(c));
                    if (r % 2 == 1) cell.setCellStyle(altStyle);
                }
            }

            // ── Auto-size columns ─────────────────────────
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            // ── Save ──────────────────────────────────────
            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }

            showSuccess("Exported to: " + file.getName());

        } catch (Exception ex) {
            showError("Export failed: " + ex.getMessage());
        }
    }

    private static void showSuccess(String msg) {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.control.Alert a =
                    new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.INFORMATION);
            a.setTitle("Export Successful");
            a.setHeaderText(null);
            a.setContentText(msg);
            a.showAndWait();
        });
    }

    private static void showError(String msg) {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.control.Alert a =
                    new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.ERROR);
            a.setTitle("Export Failed");
            a.setHeaderText(null);
            a.setContentText(msg);
            a.showAndWait();
        });
    }
}