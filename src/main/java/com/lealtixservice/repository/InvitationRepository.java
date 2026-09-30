package com.lealtixservice.repository;

import com.lealtixservice.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad Invitation.
 */
public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    Optional<Invitation> findByTokenHash(String tokenHash);

    Optional<Invitation> findByEmail(String email);

    void deleteByEmail(String email);
}

