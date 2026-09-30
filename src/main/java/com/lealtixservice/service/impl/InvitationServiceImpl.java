package com.lealtixservice.service.impl;

import com.lealtixservice.dto.PreRegistroDTO;
import com.lealtixservice.dto.RegistroDto;
import com.lealtixservice.dto.ValidateTokenResponse;
import com.lealtixservice.entity.*;
import com.lealtixservice.repository.*;
import com.lealtixservice.service.InvitationService;
import com.lealtixservice.util.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Implementación del servicio de invitaciones.
 */
@Service
public class InvitationServiceImpl implements InvitationService {

    @Autowired
    private InvitationRepository invitationRepository;
    @Autowired
    private PreRegistroRepository preRegistroRepository;
    @Autowired
    private AppUserRepository appUserRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${invitation.token.expiry-hours}")
    private int expiryHours;

    @Value("${invitation.base-url}")
    private String baseUrl;


    @Override
    @Transactional
    public String generateInvitation(PreRegistroDTO dto, String ipAddress) {

        String token = generateRandomToken();
        String tokenHash = TokenUtils.hashToken(token);

        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofHours(expiryHours));

        // Limpia invitaciones previas del email (evita duplicados/conflictos de email único)
        invitationRepository.deleteByEmail(dto.getEmail());
        invitationRepository.flush(); // ejecuta el DELETE antes del INSERT siguiente
        Invitation invitation = new Invitation();
        invitation.setEmail(dto.getEmail());
        invitation.setTokenHash(tokenHash);
        invitation.setCreatedAt(now);
        invitation.setExpiresAt(expiresAt);
        invitation.setUsedAt(null);
        invitation.setCreatedByIp(ipAddress);

        invitationRepository.save(invitation);

        return baseUrl + "/registro?register=true&token=" + token;
    }

    @Override
    public ValidateTokenResponse validateToken(String token) {
        String message = "SUCCESS";
        boolean ok = true;
        RegistroDto registroDto = new RegistroDto();
        ValidateTokenResponse response = ValidateTokenResponse.builder().build();
        String tokenHash = TokenUtils.hashToken(token);
        Invitation invitation = invitationRepository.findByTokenHash(tokenHash).orElse(null);
        if(invitation == null){
            message = "Token no válido";
            ok = false;
        }else if (invitation.getUsedAt() != null) {
            message = "Token ya usado sin Registro de Tenant";
            ok = false;
        }else if (Instant.now().isAfter(invitation.getExpiresAt())) {
            message = "Token expirado";
            ok = false;
        }else{
            response.setEmail(invitation.getEmail());
            PreRegistro preRegistro = preRegistroRepository.findByEmail(invitation.getEmail()).orElse(null);
            if(preRegistro != null) {
                registroDto.setFullName(preRegistro.getNombre());
                registroDto.setEmail(preRegistro.getEmail());
                response.setRegistroDto(registroDto);
            }
        }
        response.setOk(ok);
        response.setMessage(message);

        return response;
    }


    @Override
    public Invitation getInviteByEmail(String email) {
        return invitationRepository.findByEmail(email).orElse(null);
    }

    @Override
    public void save(Invitation invite) {
        invitationRepository.save(invite);
    }


    private String generateRandomToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
