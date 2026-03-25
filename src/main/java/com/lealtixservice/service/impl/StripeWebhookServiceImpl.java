package com.lealtixservice.service.impl;

import com.lealtixservice.config.SendGridTemplates;
import com.lealtixservice.dto.EmailDTO;
import com.lealtixservice.dto.PagoDto;
import com.lealtixservice.entity.*;
import com.lealtixservice.enums.PaymentStatus;
import com.lealtixservice.repository.PreRegistroRepository;
import com.lealtixservice.repository.TenantPaymentRepository;
import com.lealtixservice.repository.TenantRepository;
import com.lealtixservice.service.*;
import com.lealtixservice.util.DateUtils;
import com.lealtixservice.util.EncrypUtils;
import com.stripe.Stripe;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class StripeWebhookServiceImpl implements StripeWebhookService {

    public static final String COMPLETED = "COMPLETED";
    private final SendGridTemplates sendGridTemplates;

    @Value("${lealtix.dashboard.url}")
    private String baseUrlDashboard;

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private TenantPaymentRepository tenantPaymentRepository;

    @Autowired
    private PreRegistroRepository preRegistroRepository;

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private Emailservice emailservice;

    @Autowired
    private TenantRepository tenantRepository;

    public StripeWebhookServiceImpl(@Value("${stripe.api.key}") String apiKey, SendGridTemplates sendGridTemplates) {
        this.sendGridTemplates = sendGridTemplates;
        Stripe.apiKey = apiKey;
    }

    @Override
    public PagoDto handleCheckoutSessionCompleted(Event event) {
        PagoDto pagoDto = null;
        try {
            StripeObject obj = event.getDataObjectDeserializer().getObject()
                    .orElseThrow(() -> new IllegalArgumentException("Stripe object is missing"));
            if (!(obj instanceof Session)) {
                throw new IllegalArgumentException("Expected Session object");
            }
            Session session = (Session) obj;

            log.info("✅ checkout.session.completed recibido:");
            log.info("ID de sesión: {}", session.getId());
            Long userId = Long.valueOf(session.getClientReferenceId());
            AppUser user = appUserService.findById(userId).orElseThrow(
                    () -> new RuntimeException("User not found with id: " + userId)
            );

            TenantPayment tp = TenantPayment.builder().build();
            tp.setCreatedAt(LocalDateTime.now());
            tp.setEndDate(LocalDateTime.now());
            tp.setPlan("Básico"); // TODO Asignar plan básico
            tp.setStartDate(LocalDateTime.now());
            tp.setTenant(null);
            tp.setName(user.getFullName());
            tp.setUIDTenant("");
            tp.setDescription("Successful payment");
            tp.setStripeCustomerId(session.getCustomer());
            tp.setStatus(session.getPaymentStatus());
            tp.setUpdatedAt(LocalDateTime.now());
            tp.setAmount(session.getAmountTotal());
            tp.setStripePaymentId(session.getId());
            tp.setStripeMode(session.getMode());
            tp.setStripeSubscriptionId(session.getSubscription());
            try {
                if (session.getPaymentMethodConfigurationDetails() != null) {
                    tp.setStripePaymentMethodId(session.getPaymentMethodConfigurationDetails().getId());
                }
            } catch (Exception e) {
                log.debug("paymentMethodConfigurationDetails missing or not expanded: {}", e.getMessage());
            }
            tp.setUserEmail(user.getEmail());
            tp.setAppUser(user);
            tenantPaymentRepository.save(tp);

            // llena pagoDto con datos de tenantPayment
            pagoDto = new PagoDto();
            pagoDto.setName(tp.getName());
            pagoDto.setStatus(tp.getStatus());
            pagoDto.setCost(tp.getAmount().toString());
            pagoDto.setCurrency(session.getCurrency());
            pagoDto.setPaymentDate(DateUtils.formatDatefromLong(session.getCreated()));
            pagoDto.setNextPaymentDate(DateUtils.formatDatefromLongNext(session.getCreated()));
            pagoDto.setDescription(tp.getDescription());
            pagoDto.setPlan(tp.getPlan());

            PreRegistro preRegistro = preRegistroRepository.findByEmail(user.getEmail()).orElse(null);
            if (preRegistro == null) {
                throw new RuntimeException("preRegistro not found with id: " + user.getEmail());
            } else {
                preRegistro.setStatus(COMPLETED); // Payment completed
                preRegistro.setDescription("Payment completed by Stripe");
                preRegistro.setUpdatedDate(LocalDateTime.now());
                preRegistroRepository.save(preRegistro);
            }

            // Actualiza invitacion usada
            Invitation invitation = invitationService.getInviteByEmail(user.getEmail());
            if (invitation != null) {
                invitation.setUsedAt(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
                invitationService.save(invitation);
            }


            String jwtToken = tokenService.generateToken(user.getId(), user.getEmail());
            log.info("Generated JWT Token for tenantId {}: {}", user.getId(), jwtToken);
            // Enviar email de bienvenida
            EmailDTO emailDTO = EmailDTO.builder()
                    .to(user.getEmail())
                    .subject("Gracias por registrarte en Lealtix")
                    .templateId(sendGridTemplates.getWelcomeTemplate())
                    .dynamicData(Map.of(
                            "name", user.getFullName(),
                            "link", baseUrlDashboard + "/admin/wizard?token=" + jwtToken,
                            "logoUrl", "https://res.cloudinary.com/lealtix-media/image/upload/v1759897289/lealtix_logo_transp_qcp5h9.png",
                            "password", EncrypUtils.decrypPassword(user.getPasswordHash()),
                            "username", user.getEmail()
                    ))
                    .build();
            emailservice.sendEmailWithTemplate(emailDTO);

        } catch (IOException e) {
            log.error("❌ FALLO envío de email de bienvenida a {}: {}", pagoDto != null ? pagoDto.getName() : "desconocido", e.getMessage(), e);
            throw new RuntimeException("Failed to send welcome email", e);
        } catch (Exception e) {
            log.error("❌ Error procesando checkout session: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing checkout session", e);
        }

        return pagoDto;
    }

    @Override
    public void handlePaymentIntentSucceeded(Event event) {
        PagoDto pagoDto = null;
        String email = "";
        try {
            StripeObject obj = event.getDataObjectDeserializer().getObject()
                    .orElseThrow(() -> new IllegalArgumentException("Stripe object is missing"));
            if (!(obj instanceof PaymentIntent)) {
                throw new IllegalArgumentException("Expected PaymentIntent object");
            }
            PaymentIntent intent = (PaymentIntent) obj;
            String latestChargeId = intent.getLatestCharge();
            Charge charge = Charge.retrieve(latestChargeId);
            String receiptUrl = charge.getReceiptUrl();

            Long userId = Long.valueOf(intent.getMetadata().get("userId"));
            String plan = intent.getMetadata().get("plan");

            log.info("✅ checkout.session.completed recibido:");
            log.info("ID de intent: {}", intent.getId());
            AppUser user = appUserService.findById(userId).orElseThrow(
                    () -> new RuntimeException("User not found with id: " + userId)
            );
            email =  user.getEmail();
            TenantPayment tp = TenantPayment.builder().build();
            tp.setCreatedAt(LocalDateTime.now());
            tp.setEndDate(LocalDateTime.now());
            tp.setPlan(plan);
            tp.setStartDate(LocalDateTime.now());
            tp.setTenant(null);
            tp.setName(user.getFullName());
            tp.setUIDTenant(null);
            tp.setDescription("Successful payment");
            tp.setStripeCustomerId(intent.getCustomer());
            tp.setStatus(intent.getStatus());
            tp.setUpdatedAt(LocalDateTime.now());
            tp.setAmount(intent.getAmount());
            tp.setStripePaymentId(intent.getId());
            tp.setStripeMode("payment_intent");
            tp.setStripeSubscriptionId("");
            tp.setReceiptUrl(receiptUrl);
            try {
                if (intent.getPaymentMethodConfigurationDetails() != null) {
                    tp.setStripePaymentMethodId(intent.getPaymentMethodConfigurationDetails().getId());
                }
            } catch (Exception e) {
                log.debug("paymentMethodConfigurationDetails missing or not expanded: {}", e.getMessage());
            }
            tp.setUserEmail(user.getEmail());
            tp.setAppUser(user);
            tenantPaymentRepository.save(tp);

            // llena pagoDto con datos de tenantPayment
            pagoDto = new PagoDto();
            pagoDto.setName(tp.getName());
            pagoDto.setStatus(tp.getStatus());
            pagoDto.setCost(tp.getAmount().toString());
            pagoDto.setCurrency(intent.getCurrency());
            pagoDto.setPaymentDate(DateUtils.formatDatefromLong(intent.getCreated()));
            pagoDto.setNextPaymentDate(DateUtils.formatDatefromLongNext(intent.getCreated()));
            pagoDto.setDescription(tp.getDescription());
            pagoDto.setPlan(tp.getPlan());

            PreRegistro preRegistro = preRegistroRepository.findByEmail(user.getEmail()).orElse(null);
            if (preRegistro == null) {
                throw new RuntimeException("preRegistro not found with id: " + user.getEmail());
            } else {
                preRegistro.setStatus(COMPLETED); // Payment completed
                preRegistro.setDescription("Payment completed by Stripe");
                preRegistro.setUpdatedDate(LocalDateTime.now());
                preRegistroRepository.save(preRegistro);
            }

            // Actualiza invitacion usada
            Invitation invitation = invitationService.getInviteByEmail(user.getEmail());
            if (invitation != null) {
                invitation.setUsedAt(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
                invitationService.save(invitation);
            }


            String jwtToken = tokenService.generateToken(user.getId(), user.getEmail());
            log.info("Generated JWT Token for tenantId {}: {}", user.getId(), jwtToken);
            // Enviar email de bienvenida
            EmailDTO emailDTO = EmailDTO.builder()
                    .to(user.getEmail())
                    .subject("Gracias por registrarte en Lealtix")
                    .templateId(sendGridTemplates.getWelcomeTemplate())
                    .dynamicData(Map.of(
                            "name", user.getFullName(),
                            "link",  baseUrlDashboard + "/admin/wizard?token=" + jwtToken,
                            "logoUrl", "https://res.cloudinary.com/lealtix-media/image/upload/v1759897289/lealtix_logo_transp_qcp5h9.png",
                            "password", EncrypUtils.decrypPassword(user.getPasswordHash()),
                            "username", user.getEmail(),
                            "receiptUrl", receiptUrl
                    ))
                    .build();
            emailservice.sendEmailWithTemplate(emailDTO);

        } catch (IOException e) {
            log.error("❌ FALLO envío de email de bienvenida a {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Failed to send welcome email after payment intent success", e);
        } catch (Exception e) {
            log.error("❌ Error procesando payment intent succeeded: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing payment intent succeeded", e);
        }
    }

    @Override
    public void handlePaymentIntentFailed(Event event) {
        try {
            StripeObject obj = event.getDataObjectDeserializer().getObject()
                    .orElseThrow(() -> new IllegalArgumentException("Stripe object is missing"));
            if (!(obj instanceof PaymentIntent)) {
                throw new IllegalArgumentException("Expected PaymentIntent object");
            }
            PaymentIntent intent = (PaymentIntent) obj;

            log.info("❌ PaymentIntent.failed recibido: id={} amount={} currency={}", intent.getId(), intent.getAmount(), intent.getCurrency());

            // Crear registro TenantPayment indicando intento de cobro fallido
            TenantPayment tp = TenantPayment.builder().build();
            tp.setCreatedAt(LocalDateTime.now());
            tp.setEndDate(LocalDateTime.now());
            // Intentar obtener plan desde metadata si existe
            String plan = null;
            try {
                if (intent.getMetadata() != null) {
                    plan = intent.getMetadata().get("plan");
                }
            } catch (Exception ignored) {
            }
            tp.setPlan(plan != null ? plan : "N/A");
            tp.setStartDate(LocalDateTime.now());
            tp.setTenant(null);
            tp.setUIDTenant(null);
            tp.setDescription("PaymentIntent failed");
            tp.setStripeCustomerId(intent.getCustomer());
            // Marcar como FAILED usando el enum como en handleChargeFailed
            tp.setStatus(PaymentStatus.FAILED.getStatus());
            tp.setUpdatedAt(LocalDateTime.now());
            tp.setAmount(intent.getAmount());
            tp.setStripePaymentId(intent.getId());
            tp.setStripeMode("payment_intent");
            tp.setStripeSubscriptionId("");
            try {
                if (intent.getPaymentMethodConfigurationDetails() != null) {
                    tp.setStripePaymentMethodId(intent.getPaymentMethodConfigurationDetails().getId());
                } else if (intent.getPaymentMethod() != null) {
                    tp.setStripePaymentMethodId(intent.getPaymentMethod());
                }
            } catch (Exception e) {
                log.debug("paymentMethodConfigurationDetails missing or not expanded: {}", e.getMessage());
            }

            // Intentar asociar usuario si metadata contiene userId
            String userEmail = null;
            try {
                if (intent.getMetadata() != null && intent.getMetadata().get("userId") != null) {
                    Long userId = Long.valueOf(intent.getMetadata().get("userId"));
                    AppUser user = appUserService.findById(userId).orElse(null);
                    if (user != null) {
                        tp.setName(user.getFullName());
                        tp.setAppUser(user);
                        userEmail = user.getEmail();
                    }
                }
            } catch (Exception e) {
                log.debug("No se pudo asociar AppUser desde metadata: {}", e.getMessage());
            }

            // Si no se obtuvo email desde AppUser, intentar extraer desde metadata
            if (userEmail == null) {
                try {
                    if (intent.getMetadata() != null && intent.getMetadata().get("email") != null) {
                        userEmail = intent.getMetadata().get("email");
                    }
                } catch (Exception ignored) {
                }
            }
            tp.setUserEmail(userEmail);

            // Construir descripción más detallada con el último error si existe
            String lastError = null;
            try {
                if (intent.getLastPaymentError() != null) {
                    lastError = intent.getLastPaymentError().getMessage();
                }
            } catch (Exception ignored) {
            }
            if (lastError != null && !lastError.isBlank()) {
                tp.setDescription("PaymentIntent failed: " + lastError);
            }

            tenantPaymentRepository.save(tp);

            // Aquí se debe implementar el envío de email al usuario notificando el cargo fallido.
            // Por ahora solo dejamos este comentario como recordatorio.

            log.info("TenantPayment creado para intento fallido: id={} stripeId={}", tp.getId(), tp.getStripePaymentId());

        } catch (Exception e) {
            log.error("Error manejando payment_intent.failed: {}", e.getMessage(), e);
        }
    }

    @Override
    public void handleChargeFailed(Event event) {
        try {
            StripeObject obj = event.getDataObjectDeserializer().getObject()
                    .orElseThrow(() -> new IllegalArgumentException("Stripe object is missing"));
            if (!(obj instanceof Charge)) {
                log.warn("Charge.failed received but payload is not Charge, type={}", obj.getClass().getSimpleName());
                return;
            }

            Charge charge = (Charge) obj;
            String chargeId = charge.getId();
            String paymentIntentId = charge.getPaymentIntent();
            String lookupId = (paymentIntentId != null && !paymentIntentId.isBlank()) ? paymentIntentId : chargeId;

            log.info("❌ charge.failed recibido: chargeId={} paymentIntentId={} amount={} currency={}", chargeId, paymentIntentId, charge.getAmount(), charge.getCurrency());

            Optional<TenantPayment> opt = tenantPaymentRepository.findByStripePaymentId(lookupId);
            if (opt.isPresent()) {
                TenantPayment tp = opt.get();
                tp.setStatus(PaymentStatus.FAILED.getStatus());
                String failureMessage = charge.getFailureMessage() != null ? charge.getFailureMessage() : charge.getFailureCode();
                tp.setDescription("Charge failed: " + (failureMessage != null ? failureMessage : "Unknown reason"));
                tp.setUpdatedAt(LocalDateTime.now());
                tenantPaymentRepository.save(tp);

            } else {
                log.warn("No se encontró TenantPayment para stripePaymentId={}", lookupId);
            }

        } catch (Exception e) {
            log.error("Error manejando charge.failed: {}", e.getMessage(), e);
        }
    }
}