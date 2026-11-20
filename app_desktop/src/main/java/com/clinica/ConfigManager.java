package com.clinica;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class ConfigManager {
    private final Properties props = new Properties();

    public ConfigManager() throws IOException {
        Path localPath = Paths.get("config.properties");
        Path modulePath = Paths.get("app_desktop", "config.properties");
        Path targetPath = Files.exists(localPath) ? localPath : modulePath;

        if (!Files.exists(targetPath)) {
            throw new IOException("No se encontró config.properties en el proyecto.");
        }

        try (InputStream input = new FileInputStream(targetPath.toFile())) {
            props.load(input);
        }
    }

    public String get(String key) {
        String val = props.getProperty(key);
        return val == null ? "" : val.trim();
    }

    public int getInt(String key, int defaultValue) {
        String val = props.getProperty(key);
        if (val == null || val.trim().isEmpty()) {
            return defaultValue;
        }
        return Integer.parseInt(val.trim());
    }
}