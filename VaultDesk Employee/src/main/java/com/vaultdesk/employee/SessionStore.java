package com.vaultdesk.employee;

import java.io.*;
import java.util.Properties;

public class SessionStore {

    private static final String SESSION_FILE =
            ConfigManager.getAppDataPath() + "employee-session.properties";

    public static void save(int employeeId, String name, String empCode,
                            String designation, int departmentId,
                            String email, String username,
                            String passwordHash) {
        Properties props = new Properties();
        props.setProperty("employeeId",   String.valueOf(employeeId));
        props.setProperty("name",         name);
        props.setProperty("empCode",      empCode);
        props.setProperty("designation",  designation);
        props.setProperty("departmentId", String.valueOf(departmentId));
        props.setProperty("email",        email);
        props.setProperty("username",     username);
        props.setProperty("passwordHash", passwordHash);
        try (FileOutputStream fos = new FileOutputStream(SESSION_FILE)) {
            props.store(fos, "VaultDesk Employee Session");
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