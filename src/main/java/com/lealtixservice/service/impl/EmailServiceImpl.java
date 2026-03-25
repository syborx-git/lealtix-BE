package com.lealtixservice.service.impl;

import com.lealtixservice.dto.EmailAttachmentDTO;
import com.lealtixservice.dto.EmailDTO;
import com.lealtixservice.entity.EmailLog;
import com.lealtixservice.service.EmailLogService;
import com.lealtixservice.service.Emailservice;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@Slf4j
public class EmailServiceImpl implements Emailservice {

    @Autowired
    private SendGrid sendGrid;

    @Autowired
    private EmailLogService emailLogService;

    @Value("${sendgrid.email.from}")
    private String emailFrom;


    public void sendEmail(String to, String subject, String body) throws IOException {
        Email from = new Email(emailFrom);
        Email recipient = new Email(to);
        Content content = new Content("text/plain", body);
        Mail mail = new Mail(from, subject, recipient, content);

        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sendGrid.api(request);
            log.info("Status Code: {}", response.getStatusCode());
            log.info("Body: {}", response.getBody());
            log.info("Headers: {}", response.getHeaders());
        } catch (IOException ex) {
            log.error("Error sending email", ex);
            throw ex;
        }
    }

    public void sendEmailWithTemplate(EmailDTO emailDTO) throws IOException {
        log.info("📧 [EmailService] Iniciando envío de email a: {}", emailDTO.getTo());
        log.debug("[EmailService] Template ID: {}", emailDTO.getTemplateId());
        log.debug("[EmailService] Subject: {}", emailDTO.getSubject());
        log.debug("[EmailService] From: {}", emailFrom);

        try {
            Mail mail = new Mail();
            mail.setFrom(new Email(emailFrom));
            mail.setSubject(emailDTO.getSubject());
            mail.setTemplateId(emailDTO.getTemplateId());
            log.info("Sending email ... : {}", emailDTO.getSubject());
            Personalization personalization = new Personalization();
            personalization.addTo(new Email(emailDTO.getTo()));
            personalization.setSubject(emailDTO.getSubject());

            if (emailDTO.getDynamicData() != null) {
                emailDTO.getDynamicData().forEach(personalization::addDynamicTemplateData);
            }
            mail.addPersonalization(personalization);

            // Agregar attachments si existen
            if (emailDTO.getAttachments() != null && !emailDTO.getAttachments().isEmpty()) {
                log.info("Agregando {} attachment(s) al email", emailDTO.getAttachments().size());
                for (EmailAttachmentDTO attachmentDTO : emailDTO.getAttachments()) {
                    Attachments attachment = new Attachments();
                    attachment.setContent(attachmentDTO.getContent());
                    attachment.setType(attachmentDTO.getType());
                    attachment.setFilename(attachmentDTO.getFilename());
                    attachment.setDisposition(attachmentDTO.getDisposition());
                    attachment.setContentId(attachmentDTO.getContentId());
                    mail.addAttachments(attachment);
                    log.debug("Attachment agregado: {} (contentId: {})",
                        attachmentDTO.getFilename(), attachmentDTO.getContentId());
                }
            }

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);
            log.info("SendGrid response: {}", response.getStatusCode());

            // Registra en EmailLog
            EmailLog emailLog = EmailLog.builder()
                    .entityType(emailDTO.getEntityType() != null ? emailDTO.getEntityType() : emailDTO.getTemplateId())
                    .entityId(emailDTO.getEntityId() != null ? emailDTO.getEntityId() : 0L)
                    .email(emailDTO.getTo())
                    .templateName(emailDTO.getTemplateId())
                    .sendgridMessageId(response.getHeaders().get("X-Message-Id") != null ? response.getHeaders().get("X-Message-Id").trim() : null)
                    .status(response.getStatusCode() >= 200 && response.getStatusCode() < 300 ? "sent" : "failed")
                    .errorMessage(response.getStatusCode() >= 200 && response.getStatusCode() < 300 ? null : response.getBody())
                    .createdAt(LocalDateTime.now())
                    .build();
            emailLogService.save(emailLog);

            // Si la respuesta es error, lanzar excepción
            if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
                String errorMsg = String.format("SendGrid API error (status %d): %s", response.getStatusCode(), response.getBody());
                log.error("❌ {}", errorMsg);
                throw new IOException(errorMsg);
            }
            
            log.info("✅ Email enviado exitosamente a: {} | MessageId: {}", emailDTO.getTo(), 
                response.getHeaders().get("X-Message-Id"));

        } catch (IOException e) {
            // Registra en EmailLog si hay error de I/O
            EmailLog failedLog = EmailLog.builder()
                    .entityType(emailDTO.getEntityType() != null ? emailDTO.getEntityType() : emailDTO.getTemplateId())
                    .entityId(emailDTO.getEntityId() != null ? emailDTO.getEntityId() : 0L)
                    .email(emailDTO.getTo())
                    .templateName(emailDTO.getTemplateId())
                    .status("failed")
                    .errorMessage("IOException: " + e.getMessage())
                    .createdAt(LocalDateTime.now())
                    .build();
            emailLogService.save(failedLog);
            
            log.error("❌ Error de IO al enviar email a {}: {}", emailDTO.getTo(), e.getMessage(), e);
            throw e;
        }
    }


}
