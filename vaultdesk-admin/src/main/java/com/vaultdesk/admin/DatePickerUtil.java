package com.vaultdesk.admin;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DatePickerUtil {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ── Returns a TextField + calendar button combo ───────
    public static HBox dateField(TextField textField) {
        textField.setPromptText("YYYY-MM-DD");

        Button calBtn = new Button("📅");
        calBtn.setStyle(
                "-fx-background-color: #21262d;" +
                        "-fx-text-fill: #c9d1d9;" +
                        "-fx-border-color: #30363d;" +
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
        dialog.setTitle("Select Date");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

        DatePicker picker = new DatePicker();
        picker.setStyle(
                "-fx-background-color: #21262d;" +
                        "-fx-text-fill: #c9d1d9;");

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

    // ── Quick standalone picker ───────────────────────────
    public static void pick(TextField field) {
        showPicker(field);
    }
}