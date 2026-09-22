package com.vaultdesk.admin;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DatePickerUtil {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // ── Returns a TextField + calendar button combo ───────
    public static HBox dateField(TextField textField) {
        textField.setPromptText("dd-MM-yyyy");

        Button calBtn = new Button("📅");
        calBtn.getStyleClass().add("btn-primary");
        calBtn.setStyle(
                "-fx-border-radius: 0 6 6 0;" +
                        "-fx-background-radius: 0 6 6 0;" +
                        "-fx-padding: 6 10 6 10;" +
                        "-fx-cursor: hand;");

        textField.setStyle(
                textField.getStyle() +
                        "-fx-border-radius: 6 0 0 6;" +
                        "-fx-background-radius: 6 0 0 6;");

        calBtn.setOnAction(e -> showPicker(textField));
        textField.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2)
                showPicker(textField);
        });

        HBox box = new HBox(0, textField, calBtn);
        HBox.setHgrow(textField, Priority.ALWAYS);
        return box;
    }

    private static void showPicker(TextField textField) {
        Dialog<LocalDate> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog); // This will now properly apply Light or Dark CSS
        dialog.setTitle("Select Date");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

        DatePicker picker = new DatePicker();

        // Removed picker.setStyle(...) hardcoded background colors completely
        // so it defaults to the stylesheet's .combo-box / .text-field styles

        // Pre-fill if field has valid date
        try {
            if (textField.getText() != null
                    && !textField.getText().isEmpty()) {
                picker.setValue(LocalDate.parse(
                        textField.getText(), FMT));
            }
        } catch (Exception ignored) {}

        if (picker.getValue() == null)
            picker.setValue(LocalDate.now());

        VBox content = new VBox(10, picker);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK)
                return picker.getValue();
            return null;
        });

        dialog.showAndWait().ifPresent(date ->
                textField.setText(date.format(FMT)));
    }
    /** Converts the field's displayed "dd-MM-yyyy" text to "yyyy-MM-dd" for sending to the server. Returns "" if blank/unparseable. */
    public static String toIso(String ddMMyyyy) {
        if (ddMMyyyy == null || ddMMyyyy.trim().isEmpty()) return "";
        try {
            return LocalDate.parse(ddMMyyyy.trim(), FMT).toString(); // LocalDate.toString() is ISO yyyy-MM-dd
        } catch (Exception e) {
            return "";
        }
    }

    /** Converts a server-stored "yyyy-MM-dd" (or "yyyy-MM-dd HH:mm:ss") value to display "dd-MM-yyyy". Returns "" if blank/unparseable. */
    public static String fromIso(String isoOrDateTime) {
        if (isoOrDateTime == null || isoOrDateTime.trim().isEmpty()) return "";
        try {
            String datePart = isoOrDateTime.trim().substring(0, 10);
            return LocalDate.parse(datePart).format(FMT);
        } catch (Exception e) {
            return "";
        }
    }

    // ── Quick standalone picker ───────────────────────────
    public static void pick(TextField field) {
        showPicker(field);
    }
}