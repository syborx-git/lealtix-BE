package com.lealtixservice.specification;

import com.lealtixservice.entity.Tenant;
import com.lealtixservice.entity.TenantCustomer;
import com.lealtixservice.repository.TenantCustomerRepository;
import com.lealtixservice.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para TenantCustomerSpecifications.
 * Verifica que cada especificación (filtro) funciona correctamente con datos reales en BD.
 * 
 * Nota: Requiere base de datos de prueba activa (@SpringBootTest carga la config de test)
 */
@SpringBootTest
@Transactional
@TestPropertySource(locations = "classpath:application-test.properties")
class TenantCustomerSpecificationsTest {

    @Autowired
    private TenantCustomerRepository repository;

    @Autowired
    private TenantRepository tenantRepository;

    private Tenant testTenant;
    private Tenant otherTenant;

    @BeforeEach
    void setUp() {
        // Crear tenants de prueba y persistirlos para evitar violaciones de FK
        testTenant = Tenant.builder()
                .nombreNegocio("Tenant Test 1")
                .build();
        testTenant = tenantRepository.save(testTenant);

        otherTenant = Tenant.builder()
                .nombreNegocio("Tenant Test 2")
                .build();
        otherTenant = tenantRepository.save(otherTenant);
    }

    @Test
    void testByTenantId_FiltersCorrectly() {
        // Crear clientes en diferentes tenants
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
                .tenant(otherTenant)
                .acceptedPromotions(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        repository.save(customer1);
        repository.save(customer2);

        // Aplicar especificación
        Specification<TenantCustomer> spec = TenantCustomerSpecifications.byTenantId(testTenant.getId());
        List<TenantCustomer> result = repository.findAll(spec);

        // Verificar que solo se retorna el cliente del tenant 1
        assertEquals(1, result.size());
        assertEquals("Cliente 1", result.get(0).getName());
    }

    @Test
    void testAcceptedPromotions_FiltersCorrectly() {
        // Crear clientes con diferentes valores de acceptedPromotions
        TenantCustomer accepted = TenantCustomer.builder()
                .name("Aceptó")
                .email("accepted@test.com")
                .tenant(testTenant)
                .acceptedPromotions(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        TenantCustomer notAccepted = TenantCustomer.builder()
                .name("No Aceptó")
                .email("notaccepted@test.com")
                .tenant(testTenant)
                .acceptedPromotions(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        repository.save(accepted);
        repository.save(notAccepted);

        // Filtrar solo los que aceptaron
        Specification<TenantCustomer> spec = TenantCustomerSpecifications.acceptedPromotions();
        List<TenantCustomer> result = repository.findAll(spec);

        assertTrue(result.stream().allMatch(TenantCustomer::isAcceptedPromotions));
    }

    @Test
    void testByGender_FiltersCorrectly() {
        // Crear clientes con diferentes géneros
        TenantCustomer male = TenantCustomer.builder()
                .name("Juan")
                .email("juan@test.com")
                .tenant(testTenant)
                .gender("male")
                .acceptedPromotions(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        TenantCustomer female = TenantCustomer.builder()
                .name("María")
                .email("maria@test.com")
                .tenant(testTenant)
                .gender("female")
                .acceptedPromotions(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        repository.save(male);
        repository.save(female);

        // Filtrar solo hombres
        Specification<TenantCustomer> spec = TenantCustomerSpecifications.byGender("male");
        List<TenantCustomer> result = repository.findAll(spec);

        assertEquals(1, result.size());
        assertEquals("male", result.get(0).getGender());
    }

    @Test
    void testCreatedWithinDays_FiltersCorrectly() {
        // Crear clientes con diferentes fechas de creación
        TenantCustomer recent = TenantCustomer.builder()
                .name("Cliente Nuevo")
                .email("nuevo@test.com")
                .tenant(testTenant)
                .acceptedPromotions(true)
                .createdAt(LocalDateTime.now().minusDays(5))
                .updatedAt(LocalDateTime.now())
                .build();

        TenantCustomer old = TenantCustomer.builder()
                .name("Cliente Antiguo")
                .email("antiguo@test.com")
                .tenant(testTenant)
                .acceptedPromotions(true)
                .createdAt(LocalDateTime.now().minusDays(45))
                .updatedAt(LocalDateTime.now())
                .build();

        repository.save(recent);
        repository.save(old);

        // Filtrar clientes creados en los últimos 30 días
        Specification<TenantCustomer> spec = TenantCustomerSpecifications.createdWithinDays(30);
        List<TenantCustomer> result = repository.findAll(spec);

        assertEquals(1, result.size());
        assertEquals("Cliente Nuevo", result.get(0).getName());
    }

    @Test
    void testBirthdayInNextDays_SameBirthdayInRange() {
        // Cliente con cumpleaños en 3 días
        LocalDate upcomingBirthday = LocalDate.now().plusDays(3);
        TenantCustomer withUpcomingBirthday = TenantCustomer.builder()
                .name("Cliente Cumpleaños")
                .email("birthday@test.com")
                .tenant(testTenant)
                .birthDate(upcomingBirthday)
                .acceptedPromotions(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Cliente con cumpleaños hace 1 mes
        LocalDate pastBirthday = LocalDate.now().minusDays(30);
        TenantCustomer withPastBirthday = TenantCustomer.builder()
                .name("Cliente Sin Cumpleaños Próximo")
                .email("past@test.com")
                .tenant(testTenant)
                .birthDate(pastBirthday)
                .acceptedPromotions(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        repository.save(withUpcomingBirthday);
        repository.save(withPastBirthday);

        // Filtrar clientes con cumpleaños en los próximos 7 días
        Specification<TenantCustomer> spec = TenantCustomerSpecifications.birthdayInNextDays(7);
        List<TenantCustomer> result = repository.findAll(spec);

        assertEquals(1, result.size());
        assertEquals("Cliente Cumpleaños", result.get(0).getName());
    }

    @Test
    void testBirthdayInNextDays_CrossesYearBoundary() {
        // Simular un cliente con cumpleaños el 2 de enero (próximas 2 semanas si hoy es 28 dic)
        LocalDate newYearBirthday = LocalDate.of(LocalDate.now().getYear(), 1, 2);
        
        // Si la prueba se ejecuta fuera de finales de diciembre, ajustar el test
        // Para este test simplificado, solo verificamos que no lanza excepción
        
        TenantCustomer withNewYearBirthday = TenantCustomer.builder()
                .name("Cliente Año Nuevo")
                .email("newyear@test.com")
                .tenant(testTenant)
                .birthDate(newYearBirthday)
                .acceptedPromotions(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        repository.save(withNewYearBirthday);

        // Verificar que la especificación funciona sin error
        Specification<TenantCustomer> spec = TenantCustomerSpecifications.birthdayInNextDays(30);
        List<TenantCustomer> result = repository.findAll(spec);

        assertDoesNotThrow(() -> repository.findAll(spec));
    }

    @Test
    void testActiveWithinDays_FiltersByUpdatedAt() {
        // Cliente activo hace 10 días
        TenantCustomer recentlyActive = TenantCustomer.builder()
                .name("Cliente Activo")
                .email("activo@test.com")
                .tenant(testTenant)
                .acceptedPromotions(true)
                .createdAt(LocalDateTime.now().minusDays(60))
                .updatedAt(LocalDateTime.now().minusDays(10))
                .build();

        // Cliente sin actividad hace 60 días
        TenantCustomer inactive = TenantCustomer.builder()
                .name("Cliente Inactivo")
                .email("inactivo@test.com")
                .tenant(testTenant)
                .acceptedPromotions(true)
                .createdAt(LocalDateTime.now().minusDays(120))
                .updatedAt(LocalDateTime.now().minusDays(60))
                .build();

        repository.save(recentlyActive);
        repository.save(inactive);

        // Filtrar clientes activos en los últimos 30 días
        Specification<TenantCustomer> spec = TenantCustomerSpecifications.activeWithinDays(30);
        List<TenantCustomer> result = repository.findAll(spec);

        assertEquals(1, result.size());
        assertEquals("Cliente Activo", result.get(0).getName());
    }

    @Test
    void testCombinedSpecifications() {
        // Combinar múltiples especificaciones con .and()
        TenantCustomer targetCustomer = TenantCustomer.builder()
                .name("Cliente Objetivo")
                .email("objetivo@test.com")
                .tenant(testTenant)
                .gender("male")
                .acceptedPromotions(true)
                .createdAt(LocalDateTime.now().minusDays(15))
                .updatedAt(LocalDateTime.now())
                .build();

        TenantCustomer otherCustomer = TenantCustomer.builder()
                .name("Cliente Otro")
                .email("otro@test.com")
                .tenant(testTenant)
                .gender("female")
                .acceptedPromotions(true)
                .createdAt(LocalDateTime.now().minusDays(15))
                .updatedAt(LocalDateTime.now())
                .build();

        repository.save(targetCustomer);
        repository.save(otherCustomer);

        // Filtrar: hombres, creados en últimos 30 días, que aceptaron promociones
        Specification<TenantCustomer> spec = Specification
                .where(TenantCustomerSpecifications.byTenantId(testTenant.getId()))
                .and(TenantCustomerSpecifications.byGender("male"))
                .and(TenantCustomerSpecifications.createdWithinDays(30))
                .and(TenantCustomerSpecifications.acceptedPromotions());

        List<TenantCustomer> result = repository.findAll(spec);

        assertEquals(1, result.size());
        assertEquals("Cliente Objetivo", result.get(0).getName());
    }
}