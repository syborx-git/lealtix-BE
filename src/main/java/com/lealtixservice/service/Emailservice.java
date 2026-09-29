package com.lealtixservice.service;

import com.lealtixservice.dto.EmailAttachmentDTO;
import com.lealtixservice.dto.EmailDTO;

import java.io.IOException;
import java.util.List;

public interface Emailservice {


    public void sendEmail(String to, String subject, String body) throws IOException;

    public void sendEmailWithTemplate(EmailDTO emailDTO) throws IOException;

    /**
     * Envía un correo HTML (sin plantilla de SendGrid) con adjuntos.
     */
    public void sendEmailWithAttachments(String to, String subject, String htmlBody, List<EmailAttachmentDTO> attachments) throws IOException;
}
