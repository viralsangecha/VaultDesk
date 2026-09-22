package com.vaultdesk.admin;

import javafx.scene.Scene;
import javafx.scene.control.Dialog;

public class ThemeManager {

    public enum Theme { DARK, LIGHT }

    private static Theme current = Theme.DARK;

    public static Theme getCurrent() { return current; }

    public static void toggle() {
        current = (current == Theme.DARK) ? Theme.LIGHT : Theme.DARK;
    }

    public static void apply(Scene scene) {
        scene.getStylesheets().clear();

        // Our CSS on top — overrides AtlantaFX
        String base = ThemeManager.class
                .getResource("/styles.css").toExternalForm();
        scene.getStylesheets().add(base);

        if (current == Theme.LIGHT) {
            String light = ThemeManager.class
                    .getResource("/styles-light.css").toExternalForm();
            scene.getStylesheets().add(light);
        }
    }
    public static void applyToDialog(Dialog<?> dialog) {
        dialog.getDialogPane().getStylesheets().clear();
        dialog.getDialogPane().getStylesheets().add(
                ThemeManager.class.getResource("/styles.css").toExternalForm());
        if (current == Theme.LIGHT) {
            dialog.getDialogPane().getStylesheets().add(
                    ThemeManager.class.getResource("/styles-light.css").toExternalForm());
        }
    }

}