package com.lealtixservice.repository;

import com.lealtixservice.entity.ChatBotSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatBotSessionRepository extends JpaRepository<ChatBotSession, Long> {

    /**
     * Buscar sesión por sessionId
     */
    Optional<ChatBotSession> findBySessionId(String sessionId);

    /**
     * Buscar sesiones activas de un tenant
     */
    List<ChatBotSession> findByTenantIdAndStatus(Long tenantId, String status);

    /**
     * Buscar sesiones de un cliente
     */
    Page<ChatBotSession> findByCustomerId(Long customerId, Pageable pageable);

    /**
     * Buscar sesiones de un tenant (paginado)
     */
    Page<ChatBotSession> findByTenantId(Long tenantId, Pageable pageable);

    /**
     * Contar sesiones activas de un tenant
     */
    Long countByTenantIdAndStatus(Long tenantId, String status);

    /**
     * Obtener sesiones abandonadas (sin actividad en más de X minutos)
     */
    @Query("SELECT s FROM ChatBotSession s " +
           "WHERE s.tenant.id = :tenantId " +
           "AND s.status = 'ACTIVE' " +
           "AND s.lastInteractionAt < :cutoffTime")
    List<ChatBotSession> findAbandonedSessions(@Param("tenantId") Long tenantId,
                                                @Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Estadísticas de sesiones por tenant y rango de fechas
     */
    @Query("SELECT s.status, COUNT(s) FROM ChatBotSession s " +
           "WHERE s.tenant.id = :tenantId " +
           "AND s.startedAt BETWEEN :from AND :to " +
           "GROUP BY s.status")
    List<Object[]> getSessionStatsByTenant(@Param("tenantId") Long tenantId,
                                            @Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);
}
