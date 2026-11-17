package com.lealtixservice.service.impl;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.walletobjects.Walletobjects;
import com.google.api.services.walletobjects.WalletobjectsScopes;
import com.google.api.services.walletobjects.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.lealtixservice.entity.CustomerLoyaltyCard;
import com.lealtixservice.entity.Tenant;
import com.lealtixservice.entity.TenantCustomer;
import com.lealtixservice.entity.TenantLoyaltyCardConfig;
import com.lealtixservice.repository.CustomerLoyaltyCardRepository;
import com.lealtixservice.service.GoogleWalletService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleWalletServiceImpl implements GoogleWalletService {

    private final ResourceLoader resourceLoader;
    private final CustomerLoyaltyCardRepository customerCardRepository;

    @Value("${google.wallet.enabled:true}")
    private boolean walletEnabled;

    @Value("${google.wallet.issuer-id:3388000000023241643}")
    private String issuerId;

    @Value("${google.wallet.credentials-path:classpath:google-wallet-credentials.json}")
    private String credentialsPath;

    private ServiceAccountCredentials credentials;
    private Walletobjects walletClient;

    private static final String DEFAULT_LOGO_URL = "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=500&auto=format&fit=crop&q=80";

    @PostConstruct
    public void init() {
        if (!walletEnabled) {
            log.info("Google Wallet integration is disabled by configuration");
            return;
        }

        try {
            Resource resource = resourceLoader.getResource(credentialsPath);
            if (!resource.exists()) {
                log.warn("Google Wallet credentials file not found at: {}. Pass generation will be skipped.", credentialsPath);
                return;
            }

            try (InputStream is = resource.getInputStream()) {
                this.credentials = ServiceAccountCredentials.fromStream(is);
                log.info("Google Wallet Service Account initialized successfully: {}", credentials.getClientEmail());
            }

            this.walletClient = new Walletobjects.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(this.credentials.createScoped(Collections.singletonList(WalletobjectsScopes.WALLET_OBJECT_ISSUER)))
            ).setApplicationName("Lealtix-Loyalty-Platform").build();

        } catch (Exception e) {
            log.error("Failed to initialize Google Wallet credentials: {}", e.getMessage(), e);
        }
    }

    @Override
    public boolean isConfigured() {
        return walletEnabled && credentials != null && issuerId != null && !issuerId.isBlank();
    }

    @Override
    public String generateSavePassUrl(CustomerLoyaltyCard card, TenantLoyaltyCardConfig config) {
        if (!isConfigured()) {
            log.warn("Cannot generate Google Wallet save pass: service not configured");
            return null;
        }

        try {
            Long tenantId = card.getTenant().getId();
            Long customerId = card.getCustomer().getId();

            String classId = String.format("%s.tenant_%d_class", issuerId, tenantId);
            String objectId = String.format("%s.tenant_%d_cust_%d", issuerId, tenantId, customerId);

            Map<String, Object> loyaltyClass = buildLoyaltyClassMap(classId, card.getTenant(), config);
            Map<String, Object> loyaltyObject = buildLoyaltyObjectMap(objectId, classId, card, config);

            // Construct JWT claims payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("loyaltyClasses", Collections.singletonList(loyaltyClass));
            payload.put("loyaltyObjects", Collections.singletonList(loyaltyObject));

            Map<String, Object> claims = new HashMap<>();
            claims.put("iss", credentials.getClientEmail());
            claims.put("aud", "google");
            claims.put("typ", "savetowallet");
            claims.put("iat", Instant.now().getEpochSecond());
            claims.put("payload", payload);

            PrivateKey privateKey = credentials.getPrivateKey();
            String jwt = Jwts.builder()
                    .setHeaderParam("typ", "JWT")
                    .setHeaderParam("alg", "RS256")
                    .setClaims(claims)
                    .signWith(privateKey, SignatureAlgorithm.RS256)
                    .compact();

            String saveUrl = "https://pay.google.com/gp/v/save/" + jwt;

            // Save in database entity for fast access
            card.setGoogleWalletObjectId(objectId);
            card.setGoogleWalletSaveUrl(saveUrl);
            customerCardRepository.save(card);

            log.info("Generated Google Wallet pass save URL for card {} (customer {})", card.getId(), customerId);
            return saveUrl;

        } catch (Exception e) {
            log.error("Error creating Google Wallet save URL for card {}: {}", card.getId(), e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void updatePassStamps(CustomerLoyaltyCard card, TenantLoyaltyCardConfig config) {
        if (!isConfigured() || walletClient == null) {
            return;
        }

        String objectId = card.getGoogleWalletObjectId();
        if (objectId == null || objectId.isBlank()) {
            return;
        }

        try {
            int currentStamps = card.getCurrentStamps() != null ? card.getCurrentStamps() : 0;
            int totalStamps = config.getTotalStamps() != null ? config.getTotalStamps() : 8;

            LoyaltyObject patchBody = new LoyaltyObject();
            LoyaltyPoints points = new LoyaltyPoints();
            points.setLabel("Progreso de Sellos");
            points.setBalance(new LoyaltyPointsBalance().setString(String.format("%d / %d Sellos", currentStamps, totalStamps)));
            patchBody.setLoyaltyPoints(points);

            if (Boolean.TRUE.equals(card.getHasPendingReward())) {
                LoyaltyPoints rewardPoints = new LoyaltyPoints();
                rewardPoints.setLabel("¡Premio Disponible!");
                rewardPoints.setBalance(new LoyaltyPointsBalance().setString(config.getRewardTitle()));
                patchBody.setSecondaryLoyaltyPoints(rewardPoints);
            }

            walletClient.loyaltyobject().patch(objectId, patchBody).execute();
            log.info("Google Wallet real-time pass updated for objectId: {}", objectId);

        } catch (Exception e) {
            // It's normal to catch 404 if the user hasn't physically added the card to their device yet
            log.debug("Notice updating Google Wallet pass for card {}: {}", card.getId(), e.getMessage());
        }
    }

    private Map<String, Object> buildLoyaltyClassMap(String classId, Tenant tenant, TenantLoyaltyCardConfig config) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", classId);
        map.put("issuerName", tenant.getNombreNegocio() != null ? tenant.getNombreNegocio() : "Lealtix Loyalty");
        map.put("programName", config.getCardTitle() != null ? config.getCardTitle() : "Tarjeta de Sellos");
        map.put("hexBackgroundColor", config.getBgColorHex() != null ? config.getBgColorHex() : "#4338CA");
        map.put("reviewStatus", "UNDER_REVIEW");

        String logoUrl = tenant.getLogoUrl();
        if (logoUrl == null || !logoUrl.startsWith("https://")) {
            logoUrl = DEFAULT_LOGO_URL;
        }

        Map<String, Object> programLogo = new HashMap<>();
        Map<String, Object> sourceUri = new HashMap<>();
        sourceUri.put("uri", logoUrl);
        programLogo.put("sourceUri", sourceUri);
        map.put("programLogo", programLogo);

        return map;
    }

    private Map<String, Object> buildLoyaltyObjectMap(String objectId, String classId, CustomerLoyaltyCard card, TenantLoyaltyCardConfig config) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", objectId);
        map.put("classId", classId);
        map.put("state", "ACTIVE");
        map.put("accountId", "LEALTIX-" + card.getId());

        TenantCustomer customer = card.getCustomer();
        String customerName = customer != null && customer.getName() != null ? customer.getName() : "Comensal Leal";
        map.put("accountHolderName", customerName);

        // QR Barcode
        Map<String, Object> barcode = new HashMap<>();
        barcode.put("type", "QR_CODE");
        barcode.put("value", String.format("LEALTIX-CARD-%d-TENANT-%d", card.getId(), card.getTenant().getId()));
        barcode.put("alternateText", String.format("CARD #%d", card.getId()));
        map.put("barcode", barcode);

        // Stamps points module
        int currentStamps = card.getCurrentStamps() != null ? card.getCurrentStamps() : 0;
        int totalStamps = config.getTotalStamps() != null ? config.getTotalStamps() : 8;

        Map<String, Object> points = new HashMap<>();
        points.put("label", "Progreso de Sellos");
        Map<String, Object> balance = new HashMap<>();
        balance.put("string", String.format("%d / %d Sellos", currentStamps, totalStamps));
        points.put("balance", balance);
        map.put("loyaltyPoints", points);

        // Secondary Points / Reward info
        if (config.getRewardTitle() != null) {
            Map<String, Object> secPoints = new HashMap<>();
            secPoints.put("label", "Recompensa");
            Map<String, Object> secBalance = new HashMap<>();
            secBalance.put("string", config.getRewardTitle());
            secPoints.put("balance", secBalance);
            map.put("secondaryLoyaltyPoints", secPoints);
        }

        // Informative Text Modules
        List<Map<String, Object>> textModules = new ArrayList<>();

        if (config.getRewardDescription() != null && !config.getRewardDescription().isBlank()) {
            Map<String, Object> tm1 = new HashMap<>();
            tm1.put("header", "Cómo Canjear");
            tm1.put("body", config.getRewardDescription());
            textModules.add(tm1);
        }

        if (config.getTermsAndConditions() != null && !config.getTermsAndConditions().isBlank()) {
            Map<String, Object> tm2 = new HashMap<>();
            tm2.put("header", "Términos y Condiciones");
            tm2.put("body", config.getTermsAndConditions());
            textModules.add(tm2);
        }

        if (!textModules.isEmpty()) {
            map.put("textModulesData", textModules);
        }

        return map;
    }
}
