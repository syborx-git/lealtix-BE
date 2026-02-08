package com.lealtixservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LealtixServiceApplication {
    public static void main(String[] args) {
        // Force loading of DotEnvConfig to ensure System properties are set before Spring starts
        try {
            Class.forName("com.lealtixservice.config.DotEnvConfig");
        } catch (ClassNotFoundException e) {
            // If class is not present, continue without dotenv support
            System.err.println("[LealtixServiceApplication] DotEnvConfig not found: " + e.getMessage());
        }

        SpringApplication.run(LealtixServiceApplication.class, args);
    }
}