package com.vaultdesk.admin;

import java.util.regex.Pattern;

public class ValidationUtil {
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^[0-9]{10}$"); // adjust if you need country codes/extensions

    public static boolean isValidEmail(String s) {
        return s != null && EMAIL.matcher(s.trim()).matches();
    }
    public static boolean isValidPhone(String s) {
        return s != null && PHONE.matcher(s.trim().replaceAll("[\\s-]", "")).matches();
    }
    public static boolean isNotBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}