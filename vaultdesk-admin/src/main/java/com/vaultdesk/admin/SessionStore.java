package com.vaultdesk.admin;

import java.io.*;
import java.util.Properties;

public class SessionStore {

    private static final String SESSION_FILE = "vaultdesk-session.properties";

    public static void save(int userId, String fullName,
                            String role, String username,
                            String passwordHash, int deptId) {  // ← add deptId
        Properties props = new Properties();
        props.setProperty("userId",       String.valueOf(userId));
        props.setProperty("fullName",     fullName);
        props.setProperty("role",         role);
        props.setProperty("username",     username);
        props.setProperty("passwordHash", passwordHash);
        props.setProperty("deptId",       String.valueOf(deptId));  // ← ADD
        try (FileOutputStream fos = new FileOutputStream(SESSION_FILE)) {
            props.store(fos, "VaultDesk Session — do not edit");
        } catch (Exception e) {
            System.out.println("Session save error: " + e.getMessage());
        }
    }

    public static Properties load() {
        File file = new File(SESSION_FILE);
        if (!file.exists()) return null;
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            props.load(fis);
            return props;
        } catch (Exception e) {
            System.out.println("Session load error: " + e.getMessage());
            return null;
        }
    }

    public static void clear() {
        new File(SESSION_FILE).delete();
    }

    public static boolean exists() {
        return new File(SESSION_FILE).exists();
    }
}