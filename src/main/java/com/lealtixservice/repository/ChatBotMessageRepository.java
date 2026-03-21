package com.lealtixservice.repository;

import com.lealtixservice.entity.ChatBotMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatBotMessageRepository extends JpaRepository<ChatBotMessage, Long> {

    /**
     * Buscar mensajes de una sesión
     */
    List<ChatBotMessage> findBySessionIdOrderByTimestampAsc(Long sessionId);

    /**
     * Buscar mensajes de una sesión (paginado)
     */
    Page<ChatBotMessage> findBySessionId(Long sessionId, Pageable pageable);

    /**
     * Contar mensajes de una sesión
     */
    Long countBySessionId(Long sessionId);

    /**
     * Buscar mensajes por tipo en una sesión
     */
    List<ChatBotMessage> findBySessionIdAndMessageType(Long sessionId, String messageType);

    /**
     * Obtener estadísticas de mensajes por tipo
     */
    @Query("SELECT m.messageType, COUNT(m) FROM ChatBotMessage m " +
           "WHERE m.session.tenant.id = :tenantId " +
           "GROUP BY m.messageType")
    List<Object[]> getMessageStatsByType(@Param("tenantId") Long tenantId);
}
