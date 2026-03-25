package com.lealtixservice.service.impl;

import com.lealtixservice.dto.PagoDto;
import com.lealtixservice.dto.RegistroDto;
import com.lealtixservice.entity.*;
import com.lealtixservice.enums.RoleEnum;
import com.lealtixservice.repository.*;
import com.lealtixservice.service.InvitationService;
import com.lealtixservice.service.RegistroService;
import com.lealtixservice.util.EncrypUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;

@Slf4j
@Service
public class RegistroServiceImpl implements RegistroService {

    public static final String PENDING = "PENDING";
    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private TenantPaymentRepository tenantPaymentRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private InvitationService invitationService;
    @Autowired
    private PreRegistroRepository preRegistroRepository;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private TenantUserRepository tenantUserRepository;


    @Override
    @Transactional
    public AppUser register(RegistroDto dto) {
        Invitation invite = invitationService.getInviteByEmail(dto.getEmail());
        AppUser appUser = appUserRepository.findByEmail(dto.getEmail());
        if (invite == null) {
            throw new IllegalArgumentException("No invitation found for email: " + dto.getEmail());
        }
        if (invite.getUsedAt() != null) {
           if (appUser == null) {
                throw new IllegalArgumentException("User already registered with email: " + dto.getEmail());
            }
        }
        if (Instant.now().isAfter(invite.getExpiresAt())) {
            throw new IllegalArgumentException("Token expired for email: " + dto.getEmail());
        }

        // Create or update AppUser
        if(appUser != null){
           appUser.setFullName(dto.getFullName());
           appUser.setFechaNacimiento(dto.getFechaNacimiento());
           appUser.setTelefono(dto.getTelefono());
           appUser.setPasswordHash(EncrypUtils.encryptPassword(dto.getPassword()));
           appUser.setUpdatedAt(LocalDateTime.now());
        }else{
            appUser = AppUser.builder()
                .fullName(dto.getFullName())
                .fechaNacimiento(dto.getFechaNacimiento())
                .telefono(dto.getTelefono())
                .email(dto.getEmail())
                .passwordHash(EncrypUtils.encryptPassword(dto.getPassword()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        }
        appUserRepository.save(appUser);
        log.info("AppUser created/updated: {}", appUser.getEmail());

        // Get or create Tenant for the business owner (defensive pattern)
        AppUser finalAppUser = appUser;
        Tenant tenant = tenantRepository.findByAppUserId(appUser.getId())
                .orElseGet(() -> {
                    try {
                        // Create new tenant
                        Tenant newTenant = new Tenant();
                        newTenant.setAppUser(finalAppUser);
                        newTenant.setNombreNegocio(dto.getNombreNegocio() != null ? dto.getNombreNegocio() : finalAppUser.getFullName() + "'s Business");
                        newTenant.setDireccion(dto.getDireccion());
                        newTenant.setTelefono(dto.getTelefonoNegocio());
                        newTenant.setTipoNegocio(dto.getTipoNegocio() != null ? dto.getTipoNegocio() : "General");
                        newTenant.setActive(true);
                        newTenant.setCreatedAt(LocalDateTime.now());
                        newTenant.setUpdatedAt(LocalDateTime.now());
                        Tenant saved = tenantRepository.save(newTenant);
                        log.info("Tenant created for user: {}, ID: {}", finalAppUser.getEmail(), saved.getId());
                        return saved;
                    } catch (Exception e) {
                        // Constraint violation - another thread may have created it
                        log.warn("Constraint violation creating tenant, searching existing: {}", e.getMessage());
                        return tenantRepository.findByAppUserId(finalAppUser.getId())
                                .orElseThrow(() -> new IllegalArgumentException("Failed to get or create tenant"));
                    }
                });

        // Get or create TenantUser with ADMIN role (defensive pattern)
        AppUser finalAppUser1 = appUser;
        TenantUser tenantUser = tenantUserRepository.findByEmailAndTenantId(appUser.getEmail(), tenant.getId())
                .orElseGet(() -> {
                    try {
                        // Create new tenant user
                        TenantUser newTenantUser = new TenantUser();
                        newTenantUser.setTenant(tenant);
                        newTenantUser.setNombre(finalAppUser1.getFullName());
                        newTenantUser.setEmail(finalAppUser1.getEmail());
                        newTenantUser.setPasswordHash(EncrypUtils.encryptPassword(dto.getPassword()));
                        newTenantUser.setRol(RoleEnum.ADMIN);
                        newTenantUser.setActivo(true);
                        newTenantUser.setCreatedBy(finalAppUser1.getEmail());
                        newTenantUser.setUpdatedBy(finalAppUser1.getEmail());
                        newTenantUser.setCreatedAt(LocalDateTime.now());
                        newTenantUser.setUpdatedAt(LocalDateTime.now());
                        TenantUser saved = tenantUserRepository.save(newTenantUser);
                        log.info("TenantUser created with ADMIN role: {}", saved.getId());
                        return saved;
                    } catch (Exception e) {
                        // Constraint violation - another thread may have created it
                        log.warn("Constraint violation creating tenant user, searching existing: {}", e.getMessage());
                        return tenantUserRepository.findByEmailAndTenantId(finalAppUser1.getEmail(), tenant.getId())
                                .orElseThrow(() -> new IllegalArgumentException("Failed to get or create tenant user"));
                    }
                });

        // Update PreRegistro status to PENDING (awaiting payment)
        PreRegistro preRegistro = preRegistroRepository.findByEmail(dto.getEmail()).orElseThrow(
                () -> new IllegalArgumentException("Pre-registro no encontrado para email: " + dto.getEmail())
        );
        preRegistro.setStatus(PENDING);
        preRegistro.setDescription("Payment pending");
        preRegistro.setUpdatedDate(LocalDateTime.now());
        preRegistroRepository.save(preRegistro);
        log.info("PreRegistro status updated to PENDING: {}", dto.getEmail());

        return appUser;
    }

    @Override
    public void registrarPago(PagoDto dto) {
        AppUser user = appUserRepository.findByEmail(dto.getEmail());
        if (user == null) {
            throw new IllegalArgumentException("User not found with email: " + dto.getEmail());
        }
        
        Tenant tenant = tenantRepository.findByAppUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found for user: " + dto.getEmail()));
        
        TenantPayment payment = TenantPayment.builder()
                .tenant(tenant)
                .stripeCustomerId(dto.getStripeCustomerId())
                .stripeSubscriptionId(dto.getStripeSubscriptionId())
                .stripePaymentMethodId(dto.getStripePaymentMethodId())
                .plan(dto.getPlan())
                .status(dto.getStatus())
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now())
                .name(user.getFullName())
                .build();
        tenantPaymentRepository.save(payment);
        log.info("Payment registered for tenant: {}", tenant.getId());
    }
}
