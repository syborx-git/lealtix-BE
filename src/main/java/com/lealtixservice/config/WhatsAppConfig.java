package com.lealtixservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class WhatsAppConfig {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppConfig.class);

    @Value("${whatsapp.access-token}")
    private String accessToken;

    @Value("${whatsapp.phone-number-id}")
    private String phoneNumberId;

    @Value("${whatsapp.business-account-id}")
    private String businessAccountId;

    @Value("${whatsapp.api-version:v22.0}")
    private String apiVersion;

    @Value("${whatsapp.api-url:https://graph.facebook.com}")
    private String apiUrl;

    @PostConstruct
    public void init() {
        logger.info("================== WhatsApp Config Loaded ==================");
        logger.info("API URL: {}", apiUrl);
        logger.info("API Version: {}", apiVersion);
        logger.info("Phone Number ID: {}", phoneNumberId);
        logger.info("Access Token: {}", accessToken != null ? accessToken.substring(0, Math.min(20, accessToken.length())) + "..." : "NULL");
        logger.info("Message Endpoint: {}", getMessageEndpoint());
        logger.info("============================================================");
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getPhoneNumberId() {
        return phoneNumberId;
    }

    public String getBusinessAccountId() {
        return businessAccountId;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getMessageEndpoint() {
        return String.format("%s/%s/%s/messages", apiUrl, apiVersion, phoneNumberId);
    }

    @Bean
    public HttpClient whatsAppHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }
}
