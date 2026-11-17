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
        System.out.println(">>> GENERATED GOOGLE WALLET SAVE URL: " + saveUrl);
    }
}

