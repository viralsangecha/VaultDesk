package com.vaultdesk.admin;

import javafx.scene.Scene;

public class ThemeManager {

    public enum Theme { DARK, LIGHT }

    private static Theme current = Theme.DARK;

    public static Theme getCurrent() { return current; }

    public static void toggle() {
        current = (current == Theme.DARK) ? Theme.LIGHT : Theme.DARK;
    }

    public static void apply(Scene scene) {
        scene.getStylesheets().clear();
        // base styles always applied
        String base = ThemeManager.class
                .getResource("/styles.css").toExternalForm();
        scene.getStylesheets().add(base);

        if (current == Theme.LIGHT) {
            String light = ThemeManager.class
                    .getResource("/styles-light.css").toExternalForm();
            scene.getStylesheets().add(light);
        }
    }
}