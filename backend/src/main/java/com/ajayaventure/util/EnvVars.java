package com.ajayaventure.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Environment configuration loader.
 *
 * <p>Resolution order (highest to lowest precedence):
 * Java system property, OS environment variable, values from a local
 * <code>.env</code> file in the working directory.
 *
 * <p>Secrets must never be printed or committed. In production this loader is a
 * fallback; deployment secret managers inject real environment variables.</p>
 */
public final class EnvVars {

    private static final Map<String, String> FILE_VALUES = new HashMap<>();

    static {
        loadDotEnv();
    }

    private EnvVars() {
    }

    /**
     * Reads a configuration value.
     *
     * @param key          config key, e.g. {@code DB_USERNAME}
     * @param defaultValue fallback when unset
     */
    public static String get(String key, String defaultValue) {
        String systemProp = System.getProperty(key);
        if (systemProp != null && !systemProp.isBlank()) {
            return systemProp;
        }
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) {
            return env;
        }
        String fileVal = FILE_VALUES.get(key);
        if (fileVal != null && !fileVal.isBlank()) {
            return fileVal;
        }
        return defaultValue;
    }

    public static int getInt(String key, int defaultValue) {
        String value = get(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean getBool(String key, boolean defaultValue) {
        String value = get(key, null);
        if (value == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value.trim())
                || "1".equals(value.trim())
                || "yes".equalsIgnoreCase(value.trim());
    }

    private static void loadDotEnv() {
        Path envFile = findDotEnv();
        if (envFile == null) {
            return;
        }
        try {
            for (String rawLine : Files.readAllLines(envFile)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                    continue;
                }
                int idx = line.indexOf('=');
                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();
                if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                    value = value.substring(1, value.length() - 1);
                }
                FILE_VALUES.put(key, value);
            }
        } catch (IOException e) {
            System.err.println("[env] could not read .env file: " + e.getMessage());
        }
    }

    /**
     * Looks for {@code .env} in the working directory, then walks up the
     * directory tree (repo root), then the user home. Never reads committed
     * sample files (only an exact {@code .env} name).
     */
    private static Path findDotEnv() {
        Path cwd = Paths.get(".").toAbsolutePath().normalize();
        for (Path dir = cwd; dir != null; dir = dir.getParent()) {
            Path candidate = dir.resolve(".env");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        Path home = Paths.get(System.getProperty("user.home", "."));
        Path homeCandidate = home.resolve(".env");
        return Files.isRegularFile(homeCandidate) ? homeCandidate : null;
    }
}