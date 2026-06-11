package com.ski.inventory.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Načte proměnné ze souboru .env (z pracovního adresáře nebo z rodičovského)
 * a přidá je do Spring prostředí. Díky tomu funguje RESEND_API_KEY z .env
 * i při lokálním běhu (bez Dockeru).
 */
public class EnvFileLoader implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "envFile";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path envPath = findEnvFile();
        if (envPath == null || !Files.isReadable(envPath)) return;

        Map<String, Object> map = new HashMap<>();
        try {
            List<String> lines = Files.readAllLines(envPath);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                int eq = trimmed.indexOf('=');
                if (eq <= 0) continue;
                String key = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();
                if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2)
                    value = value.substring(1, value.length() - 1);
                else if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2)
                    value = value.substring(1, value.length() - 1);
                if (!key.isEmpty()) map.put(key, value);
            }
        } catch (IOException ignored) {
            return;
        }
        if (map.isEmpty()) return;

        MapPropertySource source = new MapPropertySource(PROPERTY_SOURCE_NAME, map);
        if (environment.getPropertySources().contains("systemEnvironment"))
            environment.getPropertySources().addAfter("systemEnvironment", source);
        else
            environment.getPropertySources().addFirst(source);
    }

    private static Path findEnvFile() {
        String userDir = System.getProperty("user.dir");
        if (userDir == null) return null;
        Path dir = Path.of(userDir);
        Path inCwd = dir.resolve(".env");
        if (Files.isRegularFile(inCwd)) return inCwd;
        Path inParent = dir.getParent() != null ? dir.getParent().resolve(".env") : null;
        if (inParent != null && Files.isRegularFile(inParent)) return inParent;
        return null;
    }
}
