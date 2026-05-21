package com.vaultdesk.admin;

import javafx.scene.control.TextField;

public class NumberField extends TextField {

    private final boolean allowDecimal;

    public NumberField() {
        this(false);
    }

    public NumberField(boolean allowDecimal) {
        this.allowDecimal = allowDecimal;
    }

    @Override
    public void replaceText(int start, int end, String text) {
        if (validate(text)) super.replaceText(start, end, text);
    }

    @Override
    public void replaceSelection(String text) {
        if (validate(text)) super.replaceSelection(text);
    }

    private boolean validate(String text) {
        if (text.isEmpty()) return true;
        if (allowDecimal) {
            return text.matches("[0-9]*\\.?[0-9]*");
        }
        return text.matches("[0-9]*");
    }

    public int getIntValue() {
        try {
            return Integer.parseInt(getText().trim());
        } catch (NumberFormatException e) { return 0; }
    }

    public double getDoubleValue() {
        try {
            return Double.parseDouble(getText().trim());
        } catch (NumberFormatException e) { return 0.0; }
    }
}