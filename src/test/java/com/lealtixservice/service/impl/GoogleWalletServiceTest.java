package com.lealtixservice.service.impl;

import com.lealtixservice.entity.CustomerLoyaltyCard;
import com.lealtixservice.entity.Tenant;
import com.lealtixservice.entity.TenantCustomer;
import com.lealtixservice.entity.TenantLoyaltyCardConfig;
import com.lealtixservice.repository.CustomerLoyaltyCardRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

class GoogleWalletServiceTest {

    @Test
    void testGenerateSavePassUrl() {
        CustomerLoyaltyCardRepository repo = Mockito.mock(CustomerLoyaltyCardRepository.class);
        Mockito.when(repo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        GoogleWalletServiceImpl service = new GoogleWalletServiceImpl(new DefaultResourceLoader(), repo);

        ReflectionTestUtils.setField(service, "walletEnabled", true);
        ReflectionTestUtils.setField(service, "issuerId", "3388000000023241643");
        ReflectionTestUtils.setField(service, "credentialsPath", "classpath:google-wallet-credentials.json");

        service.init();

        assertTrue(service.isConfigured(), "Google Wallet Service should be configured");

        Tenant tenant = Tenant.builder().id(1L).nombreNegocio("Restaurante Petra").build();
        TenantCustomer customer = TenantCustomer.builder().id(1L).name("Jorge Martínez").build();
        CustomerLoyaltyCard card = CustomerLoyaltyCard.builder()
                .id(1L)
                .tenant(tenant)
                .customer(customer)
                .currentStamps(2)
                .build();

        TenantLoyaltyCardConfig config = TenantLoyaltyCardConfig.builder()
                .cardTitle("Petra Club Gourmet")
                .bgColorHex("#4338CA")
                .totalStamps(8)
                .rewardTitle("Enchiladas de Cortesía")
                .build();

        String saveUrl = service.generateSavePassUrl(card, config);

        assertNotNull(saveUrl, "Save URL should not be null");
        assertTrue(saveUrl.startsWith("https://pay.google.com/gp/v/save/"), "URL must start with Google Pay save prefix");

        String jwt = saveUrl.replace("https://pay.google.com/gp/v/save/", "");
        String[] parts = jwt.split("\\.");
        String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(payloadJson.contains("heroImage"), "JWT payload must contain heroImage for visual stamps banner");
        assertTrue(payloadJson.contains("/api/loyalty-card/banner/1/1.png"), "JWT payload heroImage must target banner endpoint");

        System.out.println(">>> GENERATED GOOGLE WALLET SAVE URL: " + saveUrl);
        System.out.println(">>> PAYLOAD: " + payloadJson);
    }

    @Test
    void testDirectPatchPassOnGoogle() {
        CustomerLoyaltyCardRepository repo = Mockito.mock(CustomerLoyaltyCardRepository.class);
        GoogleWalletServiceImpl service = new GoogleWalletServiceImpl(new DefaultResourceLoader(), repo);
        ReflectionTestUtils.setField(service, "walletEnabled", true);
        ReflectionTestUtils.setField(service, "issuerId", "3388000000023241643");
        ReflectionTestUtils.setField(service, "credentialsPath", "classpath:google-wallet-credentials.json");
        service.init();

        Tenant tenant = Tenant.builder().id(1L).nombreNegocio("Tenant Demo").build();
        TenantCustomer customer = TenantCustomer.builder().id(2L).name("Jorge").build();
        CustomerLoyaltyCard card = CustomerLoyaltyCard.builder()
                .id(3L)
                .tenant(tenant)
                .customer(customer)
                .currentStamps(2)
                .googleWalletObjectId("3388000000023241643.tenant_1_cust_2")
                .build();

        TenantLoyaltyCardConfig config = TenantLoyaltyCardConfig.builder()
                .cardTitle("Petra Club Gourmet")
                .bgColorHex("#831843")
                .totalStamps(6)
                .rewardTitle("Enchiladas Petra de Cortesía")
                .build();

        service.updatePassStamps(card, config);
    }
}

