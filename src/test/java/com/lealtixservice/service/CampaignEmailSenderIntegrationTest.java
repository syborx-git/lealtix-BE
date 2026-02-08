package com.lealtixservice.service;

import com.lealtixservice.entity.*;
import com.lealtixservice.enums.CampaignEmailStatus;
import com.lealtixservice.enums.CampaignStatus;
import com.lealtixservice.repository.CampaignEmailRepository;
import com.lealtixservice.repository.CampaignRepository;
import com.lealtixservice.repository.TenantCustomerRepository;
import com.lealtixservice.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de integración para verificar que CampaignEmailSender.sendEmailById
 * resuelve correctamente el LazyInitializationException al recargar
 * CampaignEmail con JOIN FETCH de Campaign y PromotionReward.
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("CampaignEmailSender - LazyInitializationException Fix Integration Test")
class CampaignEmailSenderIntegrationTest {

    @Autowired
    private CampaignEmailSender campaignEmailSender;

    @Autowired
    private CampaignEmailRepository campaignEmailRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantCustomerRepository tenantCustomerRepository;

    @MockBean
    private Emailservice emailService; // Mockeado para evitar envío real

    private Tenant testTenant;
    private Campaign testCampaign;
    private TenantCustomer testCustomer;

    @BeforeEach
    void setUp() {
        // Crear tenant de prueba
        testTenant = Tenant.builder()
                .nombreNegocio("Test Business")
                .slug("test-business")
                .logoUrl("https://example.com/logo.png")
                .build();
        testTenant = tenantRepository.save(testTenant);

        // Crear campaña de prueba con asociación lazy a PromotionReward
        testCampaign = Campaign.builder()
                .businessId(testTenant.getId())
                .title("Test Campaign")
                .subtitle("Test Subtitle")
                .description("Test Description")
                .callToAction("Test CTA")
                .imageUrl("https://example.com/image.png")
                .status(CampaignStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testCampaign = campaignRepository.save(testCampaign);

        // Crear cliente de prueba
        testCustomer = TenantCustomer.builder()
                .name("Test Customer")
                .email("test@example.com")
                .tenant(testTenant)
                .acceptedPromotions(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testCustomer = tenantCustomerRepository.save(testCustomer);
    }

    @Test
    @DisplayName("sendEmailById debe cargar Campaign con JOIN FETCH sin LazyInitializationException")
    @Transactional
    void testSendEmailById_NoLazyInitializationException() {
        // Arrange: Crear CampaignEmail PENDING
        CampaignEmail campaignEmail = CampaignEmail.builder()
                .campaign(testCampaign)
                .recipientEmail(testCustomer.getEmail())
                .recipientName(testCustomer.getName())
                .status(CampaignEmailStatus.PENDING)
                .attemptCount(0)
                .createdAt(LocalDateTime.now())
                .build();
        campaignEmail = campaignEmailRepository.save(campaignEmail);
        UUID emailId = campaignEmail.getId();

        // Act: Llamar sendEmailById (simula el flujo del scheduler)
        // Este método debe recargar la entidad con JOIN FETCH y NO debe lanzar LazyInitializationException
        assertDoesNotThrow(() -> {
            campaignEmailSender.sendEmailById(emailId);
        }, "sendEmailById NO debe lanzar LazyInitializationException");

        // Assert: Verificar que el estado cambió (SENT o FAILED según mock)
        CampaignEmail updatedEmail = campaignEmailRepository.findById(emailId).orElseThrow();
        assertNotNull(updatedEmail.getLastAttemptAt(), "lastAttemptAt debe estar actualizado");
        assertTrue(updatedEmail.getAttemptCount() > 0, "attemptCount debe haberse incrementado");
    }

    @Test
    @DisplayName("findByIdWithCampaignFetch debe retornar CampaignEmail con Campaign inicializado")
    @Transactional
    void testFindByIdWithCampaignFetch_CampaignInitialized() {
        // Arrange: Crear CampaignEmail
        CampaignEmail campaignEmail = CampaignEmail.builder()
                .campaign(testCampaign)
                .recipientEmail("fetch-test@example.com")
                .status(CampaignEmailStatus.PENDING)
                .attemptCount(0)
                .createdAt(LocalDateTime.now())
                .build();
        campaignEmail = campaignEmailRepository.save(campaignEmail);
        UUID emailId = campaignEmail.getId();

        // Act: Cargar con JOIN FETCH
        CampaignEmail fetchedEmail = campaignEmailRepository.findByIdWithCampaignFetch(emailId).orElseThrow();

        // Assert: Verificar que Campaign está inicializado (no es proxy)
        assertNotNull(fetchedEmail.getCampaign(), "Campaign debe estar presente");
        assertDoesNotThrow(() -> {
            Long businessId = fetchedEmail.getCampaign().getBusinessId();
            String title = fetchedEmail.getCampaign().getTitle();
            assertNotNull(businessId, "businessId debe estar accesible sin LazyInitializationException");
            assertEquals("Test Campaign", title, "title debe coincidir");
        }, "Acceder a propiedades de Campaign NO debe lanzar LazyInitializationException");
    }

    @Test
    @DisplayName("sendEmailById debe lanzar EntityNotFoundException si el ID no existe")
    void testSendEmailById_EntityNotFound() {
        // Arrange: ID inexistente
        UUID nonExistentId = UUID.randomUUID();

        // Act & Assert: Debe lanzar EntityNotFoundException
        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> {
            campaignEmailSender.sendEmailById(nonExistentId);
        }, "sendEmailById debe lanzar EntityNotFoundException para ID inexistente");
    }
}
