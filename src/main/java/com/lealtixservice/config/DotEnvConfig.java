package com.lealtixservice.config;

import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Configuration class to load environment variables from .env or environment.env file.
 * This ensures that all variables are properly loaded before Spring Boot initializes.
 *
 * Priority order:
 * 1. System environment variables (highest priority)
 * 2. .env file in project root
 * 3. environment.env file in project root
 * 4. Default values (lowest priority)
 */
@Configuration
public class DotEnvConfig {

    static {
        try {
            Path envPath = null;

            Path p1 = Paths.get(".env");
            Path p2 = Paths.get("environment.env");

            if (Files.exists(p1)) {
                envPath = p1;
                System.out.println("[DotEnvConfig] Found .env file in project root");
            } else if (Files.exists(p2)) {
                envPath = p2;
                System.out.println("[DotEnvConfig] Found environment.env file in project root");
            } else {
                System.out.println("[DotEnvConfig] No .env or environment.env file found in project root");
            }

            List<String> lines = Files.readAllLines(envPath, StandardCharsets.UTF_8);
            int loadedCount = 0;

            for (String rawLine : lines) {
                if (rawLine == null) continue;
                String line = rawLine.trim();
                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) continue;

                // Split on first '=' to allow '=' in the value
                int idx = line.indexOf('=');
                if (idx <= 0) continue; // invalid line

                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();

                // Remove surrounding single or double quotes if present
                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                    if (value.length() >= 2) {
                        value = value.substring(1, value.length() - 1);
                    }
                }

                // Only set if not already defined in system properties or env vars
                if (System.getProperty(key) == null && System.getenv(key) == null) {
                    System.setProperty(key, value);
                    loadedCount++;
                }
            }

            System.out.println("[DotEnvConfig] Successfully loaded " + loadedCount + " environment variables from " + envPath.getFileName());

        } catch (Exception e) {
            System.err.println("[DotEnvConfig] Error loading environment variables: " + e.getMessage());
            e.printStackTrace();
        }
    }
}