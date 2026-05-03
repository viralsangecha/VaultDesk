package com.vaultdesk.admin;

import java.io.*;
import java.util.Properties;

public class ConfigManager {

    private static final String CONFIG_FILE = "vaultdesk-config.properties";
    private static Properties props = new Properties();

    static {
        load();
    }

    private static void load() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
            } catch (Exception e) {
                System.out.println("Config load error: " + e.getMessage());
            }
        }
    }

    public static void save() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            props.store(fos, "VaultDesk Configuration");
        } catch (Exception e) {
            System.out.println("Config save error: " + e.getMessage());
        }
    }

    public static String getHost() {
        return props.getProperty("server.host", "");
    }

    public static String getPort() {
        return props.getProperty("server.port", "8080");
    }

    public static String getBaseUrl() {
        String host = getHost();
        String port = getPort();
        if (host.isEmpty()) return "http://localhost:8080";
        return "http://" + host + ":" + port;
    }

    public static void setHost(String host) {
        props.setProperty("server.host", host);
    }

    public static void setPort(String port) {
        props.setProperty("server.port", port);
    }

    public static boolean isConfigured() {
        return !getHost().isEmpty();
    }
}