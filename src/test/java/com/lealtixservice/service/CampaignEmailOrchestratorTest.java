package com.lealtixservice.service;

import com.lealtixservice.entity.*;
import com.lealtixservice.enums.CampaignEmailStatus;
import com.lealtixservice.enums.CampaignStatus;
import com.lealtixservice.enums.SegmentationType;
import com.lealtixservice.repository.CampaignEmailRepository;
import com.lealtixservice.repository.CampaignRepository;
import com.lealtixservice.repository.TenantCustomerRepository;
import com.lealtixservice.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para CampaignEmailOrchestrator.
 * Verifica que la generación de emails con segmentación funciona correctamente.
 */
@SpringBootTest
@Transactional
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("CampaignEmailOrchestrator - Tests")
class CampaignEmailOrchestratorTest {

    @Autowired
    private CampaignEmailOrchestrator orchestrator;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CampaignEmailRepository campaignEmailRepository;

    @Autowired
    private TenantCustomerRepository customerRepository;

    @Autowired
    private TenantRepository tenantRepository;

    private Tenant testTenant;
    private Campaign testCampaign;

    @BeforeEach
    void setUp() {
        // Crear y persistir tenant
        testTenant = Tenant.builder()
                .nombreNegocio("Test Tenant")
                .build();
        testTenant = tenantRepository.save(testTenant);

        // Crear campaña y asignar businessId al tenant persistido
        testCampaign = Campaign.builder()
                .businessId(testTenant.getId())
                .title("Test Campaign")
                .status(CampaignStatus.READY)
                .segmentation(SegmentationType.ALL.getValue())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Segmentación ALL: debe incluir todos los clientes que aceptaron promociones")
    void testSegmentationAll() {
        // Guardar campaña
        campaignRepository.save(testCampaign);

        // Crear clientes de prueba
        TenantCustomer customer1 = TenantCustomer.builder()
                .name("Cliente 1")
                .email("cliente1@test.com")
                .tenant(testTenant)
                .acceptedPromotions(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        TenantCustomer customer2 = TenantCustomer.builder()
                .name("Cliente 2")
                .email("cliente2@test.com")
                .tenant(testTenant)
                .acceptedPromotions(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        TenantCustomer customer3 = TenantCustomer.builder()
                .name("Cliente 3 (sin promociones)")
                .email("cliente3@test.com")
                .tenant(testTenant)
                .acceptedPromotions(false) // No aceptó
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        customerRepository.saveAll(List.of(customer1, customer2, customer3));

        // Iniciar campaña
        orchestrator.startCampaign(testCampaign.getId());

        // Verificar que se crearon emails solo para clientes que aceptaron
        List<CampaignEmail> emails = campaignEmailRepository.findByCampaignId(testCampaign.getId());
        assertEquals(2, emails.size());
        assertTrue(emails.stream().allMatch(e -> 
            e.getRecipientEmail().equals("cliente1@test.com") || 
            e.getRecipientEmail().equals("cliente2@test.com")
        ));
    }

    @Test
    @DisplayName("Segmentación MALE: debe filtrar solo hombres")
    void testSegmentationMale() {
        testCampaign.setSegmentation(SegmentationType.MALE.getValue());
        campaignRepository.save(testCampaign);

        // Crear clientes
        TenantCustomer male1 = TenantCustomer.builder()
                .name("Juan")
                .email("juan@test.com")
                .gender("male")
                .tenant(testTenant)
                .acceptedPromotions(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        TenantCustomer male2 = TenantCustomer.builder()
                .name("Carlos")
                .email("carlos@test.com")
                .gender("male")
                .tenant(testTenant)
                .acceptedPromotions(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        TenantCustomer female = TenantCustomer.builder()
                .name("María")
                .email("maria@test.com")
                .gender("female")
                .tenant(testTenant)
                .acceptedPromotions(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        customerRepository.saveAll(List.of(male1, male2, female));

        // Iniciar campaña
        orchestrator.startCampaign(testCampaign.getId());

        // Verificar que se crearon emails solo para hombres
        List<CampaignEmail> emails = campaignEmailRepository.findByCampaignId(testCampaign.getId());
        assertEquals(2, emails.size());
        assertTrue(emails.stream().allMatch(e -> 
            e.getRecipientEmail().equals("juan@test.com") || 
            e.getRecipientEmail().equals("carlos@test.com")
        ));
    }

    @Test
    @DisplayName("Segmentación FEMALE: debe filtrar solo mujeres")
    void testSegmentationFemale() {
        testCampaign.setSegmentation(SegmentationType.FEMALE.getValue());
        campaignRepository.save(testCampaign);

        // Crear clientes
        TenantCustomer female1 = TenantCustomer.builder()
                .name("María")
                .email("maria@test.com")
                .gender("female")
                .tenant(testTenant)
                .acceptedPromotions(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        TenantCustomer male = TenantCustomer.builder()
                .name("Juan")
                .email("juan@test.com")
                .gender("male")
                .tenant(testTenant)
                .acceptedPromotions(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        customerRepository.saveAll(List.of(female1, male));

        // Iniciar campaña
        orchestrator.startCampaign(testCampaign.getId());

        // Verificar que se creó email solo para mujeres
        List<CampaignEmail> emails = campaignEmailRepository.findByCampaignId(testCampaign.getId());
        assertEquals(1, emails.size());
        assertEquals("maria@test.com", emails.get(0).getRecipientEmail());
    }

    @Test
    @DisplayName("Segmentación NEW_30D: debe filtrar clientes nuevos (últimos 30 días)")
    void testSegmentationNew30Days() {
        testCampaign.setSegmentation(SegmentationType.NEW_30D.getValue());
        campaignRepository.save(testCampaign);

        // Crear clientes nuevos y antiguos
        TenantCustomer newCustomer = TenantCustomer.builder()
                .name("Cliente Nuevo")
                .email("nuevo@test.com")
                .tenant(testTenant)
                .acceptedPromotions(true)
                .createdAt(LocalDateTime.now().minusDays(5))
                .updatedAt(LocalDateTime.now())
                .build();

        TenantCustomer oldCustomer = TenantCustomer.builder()
                .name("Cliente Antiguo")
                .email("antiguo@test.com")
                .tenant(testTenant)
                .acceptedPromotions(true)
                .createdAt(LocalDateTime.now().minusDays(45))
                .updatedAt(LocalDateTime.now())
                .build();

        customerRepository.saveAll(List.of(newCustomer, oldCustomer));

        // Iniciar campaña
        orchestrator.startCampaign(testCampaign.getId());

        // Verificar que se creó email solo para el cliente nuevo
        List<CampaignEmail> emails = campaignEmailRepository.findByCampaignId(testCampaign.getId());
        assertEquals(1, emails.size());
        assertEquals("nuevo@test.com", emails.get(0).getRecipientEmail());
    }

    @Test
    @DisplayName("Segmentación ACTIVE_30D: debe filtrar clientes activos en últimos 30 días")
    void testSegmentationActive30Days() {
        testCampaign.setSegmentation(SegmentationType.ACTIVE_30D.getValue());
        campaignRepository.save(testCampaign);

        // Crear clientes activos e inactivos
        TenantCustomer activeCustomer = TenantCustomer.builder()
                .name("Cliente Activo")
                .email("activo@test.com")
                .tenant(testTenant)
                .acceptedPromotions(true)
                .createdAt(LocalDateTime.now().minusDays(60))
                .updatedAt(LocalDateTime.now().minusDays(10)) // Activo hace 10 días
                .build();

        TenantCustomer inactiveCustomer = TenantCustomer.builder()
                .name("Cliente Inactivo")
                .email("inactivo@test.com")
                .tenant(testTenant)
                .acceptedPromotions(true)
                .createdAt(LocalDateTime.now().minusDays(120))
                .updatedAt(LocalDateTime.now().minusDays(60)) // Inactivo hace 60 días
                .build();

        customerRepository.saveAll(List.of(activeCustomer, inactiveCustomer));

        // Iniciar campaña
        orchestrator.startCampaign(testCampaign.getId());

        // Verificar que se creó email solo para el cliente activo
        List<CampaignEmail> emails = campaignEmailRepository.findByCampaignId(testCampaign.getId());
        assertEquals(1, emails.size());
        assertEquals("activo@test.com", emails.get(0).getRecipientEmail());
    }

    @Test
    @DisplayName("Segmentación UPCOMING_BIRTHDAY_7D: debe filtrar clientes con cumpleaños próximos")
    void testSegmentationUpcomingBirthday() {
        testCampaign.setSegmentation(SegmentationType.UPCOMING_BIRTHDAY_7D.getValue());
        campaignRepository.save(testCampaign);

        // Crear clientes con cumpleaños próximos y lejanos
        LocalDate upcomingBirthday = LocalDate.now().plusDays(3);
        TenantCustomer upcomingBirthdayCustomer = TenantCustomer.builder()
                .name("Cliente Cumpleaños")
                .email("birthday@test.com")
                .birthDate(upcomingBirthday)
                .tenant(testTenant)
                .acceptedPromotions(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        LocalDate distantBirthday = LocalDate.now().plusDays(30);
        TenantCustomer distantBirthdayCustomer = TenantCustomer.builder()
                .name("Cliente Sin Cumpleaños Próximo")
                .email("nodbirthday@test.com")
                .birthDate(distantBirthday)
                .tenant(testTenant)
                .acceptedPromotions(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        customerRepository.saveAll(List.of(upcomingBirthdayCustomer, distantBirthdayCustomer));

        // Iniciar campaña
        orchestrator.startCampaign(testCampaign.getId());

        // Verificar que se creó email solo para el cliente con cumpleaños próximo
        List<CampaignEmail> emails = campaignEmailRepository.findByCampaignId(testCampaign.getId());
        assertEquals(1, emails.size());
        assertEquals("birthday@test.com", emails.get(0).getRecipientEmail());
    }

    @Test
    @DisplayName("Campaña no READY: debe lanzar excepción")
    void testStartCampaignNotReady() {
        testCampaign.setStatus(CampaignStatus.DRAFT);
        campaignRepository.save(testCampaign);

        // Intentar iniciar campaña que no está READY
        assertThrows(IllegalStateException.class, () -> 
            orchestrator.startCampaign(testCampaign.getId())
        );
    }

    @Test
    @DisplayName("CampaignEmail debe tener status PENDING y attemptCount 0")
    void testCampaignEmailDefaults() {
        campaignRepository.save(testCampaign);

        TenantCustomer customer = TenantCustomer.builder()
                .name("Cliente")
                .email("cliente@test.com")
                .tenant(testTenant)
                .acceptedPromotions(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        customerRepository.save(customer);

        orchestrator.startCampaign(testCampaign.getId());

        List<CampaignEmail> emails = campaignEmailRepository.findByCampaignId(testCampaign.getId());
        assertEquals(1, emails.size());
        
        CampaignEmail email = emails.get(0);
        assertEquals(CampaignEmailStatus.PENDING, email.getStatus());
        assertEquals(0, email.getAttemptCount());
        assertEquals(3, email.getMaxAttempts());
        assertNotNull(email.getScheduledAt());
    }
}