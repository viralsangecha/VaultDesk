package com.vaultdesk.admin;

import javafx.geometry.Bounds;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SearchablePickerField extends VBox {

    private final TextField textField = new TextField();
    private final ListView<PickerOption> suggestions = new ListView<>();
    private final Popup popup = new Popup();

    private List<PickerOption> allOptions;
    private PickerOption selected;
    private Consumer<PickerOption> onSelect;
    private boolean suppressFilter = false;

    public SearchablePickerField(List<PickerOption> options, String promptText) {
        this.allOptions = options;

        textField.setPromptText(promptText);
        textField.getStyleClass().add("text-field");
        suggestions.setPrefHeight(160);
        suggestions.getStyleClass().add("table-wrapper");

        popup.getContent().add(suggestions);
        popup.setAutoHide(true);

        textField.textProperty().addListener((obs, oldV, newV) -> {
            if (suppressFilter) return;
            selected = null; // typing invalidates previous selection
            if (newV == null || newV.trim().isEmpty()) {
                popup.hide();
                return;
            }
            String lower = newV.toLowerCase();
            List<PickerOption> matches = allOptions.stream()
                    .filter(o -> o.name.toLowerCase().contains(lower))
                    .collect(Collectors.toList());
            suggestions.getItems().setAll(matches);
            if (!matches.isEmpty()) {
                showPopup();
            } else {
                popup.hide();
            }
        });

        suggestions.setOnMouseClicked(e -> {
            PickerOption chosen = suggestions.getSelectionModel().getSelectedItem();
            if (chosen != null) choose(chosen);
        });

        textField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DOWN && !suggestions.getItems().isEmpty()) {
                suggestions.requestFocus();
                suggestions.getSelectionModel().select(0);
            } else if (e.getCode() == KeyCode.ENTER
                    && popup.isShowing() && !suggestions.getItems().isEmpty()) {
                choose(suggestions.getItems().get(0));
            } else if (e.getCode() == KeyCode.ESCAPE) {
                popup.hide();
            }
        });

        getChildren().add(textField);
    }

    private void showPopup() {
        if (textField.getScene() == null || textField.getScene().getWindow() == null) return;
        Bounds b = textField.localToScreen(textField.getBoundsInLocal());
        if (b == null) return;
        if (!popup.isShowing()) {
            popup.show(textField, b.getMinX(), b.getMaxY());
        }
        popup.setWidth(Math.max(200, textField.getWidth()));
    }

    private void choose(PickerOption option) {
        selected = option;
        suppressFilter = true;
        textField.setText(option.name);
        suppressFilter = false;
        popup.hide();
        if (onSelect != null) onSelect.accept(option);
    }

    public void setOnSelect(Consumer<PickerOption> cb) {
        this.onSelect = cb;
    }

    public void setOptions(List<PickerOption> options) {
        this.allOptions = options;
    }

    /** Pre-fill from an existing asset's saved id, without triggering onSelect (avoids re-triggering dept auto-fill on dialog open). */
    public void preselectSilently(int id) {
        allOptions.stream().filter(o -> o.id == id).findFirst().ifPresent(o -> {
            selected = o;
            suppressFilter = true;
            textField.setText(o.name);
            suppressFilter = false;
        });
    }

    /** Select by id and fire onSelect — used to auto-fill department after employee choice. */
    public void selectById(int id) {
        allOptions.stream().filter(o -> o.id == id).findFirst().ifPresent(this::choose);
    }

    public int getSelectedId() {
        return selected != null ? selected.id : 0;
    }

    public String getSelectedName() {
        return selected != null ? selected.name : "";
    }
    /** Raw text currently in the field, regardless of whether it matches a known option.
     *  Use this for free-text-with-suggestions fields (e.g. a vendor name stored as plain text, not an FK). */
    public String getText() {
        return textField.getText() == null ? "" : textField.getText().trim();
    }

    /** Pre-fill with raw text (not an id lookup) — for free-text fields like a stored vendor name.
     *  Still resolves getSelectedId()/getSelectedName() if the text happens to match a known option. */
    public void setTextSilently(String text) {
        suppressFilter = true;
        textField.setText(text == null ? "" : text);
        suppressFilter = false;
        selected = null;
        if (text != null && !text.trim().isEmpty()) {
            allOptions.stream()
                    .filter(o -> o.name.equalsIgnoreCase(text.trim()))
                    .findFirst()
                    .ifPresent(o -> selected = o);
        }
    }
}