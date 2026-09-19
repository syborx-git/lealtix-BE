package com.lealtixservice.service;

import com.lealtixservice.dto.AddSeatRequest;
import com.lealtixservice.dto.AssignItemsRequest;
import com.lealtixservice.dto.ComandaAsientoDTO;
import com.lealtixservice.dto.UpdateSeatAliasRequest;

import java.util.List;
import java.util.UUID;

/**
 * Servicio de gestión de asientos/personas de una comanda (sub-comandas backend).
 */
public interface ComandaAsientoService {

    /**
     * Lista los asientos de una comanda.
     */
    List<ComandaAsientoDTO> listSeats(UUID orderId);

    /**
     * Añade un asiento/persona a la comanda.
     */
    ComandaAsientoDTO addSeat(UUID orderId, AddSeatRequest request);

    /**
     * Renombra el alias (persona) de un asiento.
     */
    ComandaAsientoDTO renameSeat(UUID seatId, UpdateSeatAliasRequest request);

    /**
     * Asigna ítems de la comanda a un asiento y recalcula su total.
     */
    List<ComandaAsientoDTO> assignItems(UUID seatId, AssignItemsRequest request);

    /**
     * Quita un asiento de la comanda (y sus ítems asignados).
     */
    void deleteSeat(UUID seatId);
}