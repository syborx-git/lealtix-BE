package com.lealtixservice.service.impl;

import com.lealtixservice.dto.ClientOrderItemDTO;
import com.lealtixservice.entity.ClientOrderItem;
import com.lealtixservice.exception.ResourceNotFoundException;
import com.lealtixservice.mapper.ClientOrderItemMapper;
import com.lealtixservice.repository.ClientOrderItemRepository;
import com.lealtixservice.service.ClientOrderItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ClientOrderItemServiceImpl implements ClientOrderItemService {

    private final ClientOrderItemRepository clientOrderItemRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<ClientOrderItemDTO> getItemById(UUID itemId) {
        return clientOrderItemRepository.findById(itemId)
                .map(ClientOrderItemMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientOrderItemDTO> getItemsByOrder(UUID orderId) {
        log.debug("Obteniendo items de la orden: {}", orderId);
        return clientOrderItemRepository.findByOrderId(orderId)
                .stream()
                .map(ClientOrderItemMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ClientOrderItemDTO updateItem(UUID itemId, ClientOrderItemDTO dto) {
        log.info("Actualizando item: {}", itemId);

        ClientOrderItem item = clientOrderItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item no encontrado con ID: " + itemId));

        ClientOrderItemMapper.updateEntity(dto, item);
        item = clientOrderItemRepository.save(item);

        log.info("Item {} actualizado exitosamente", itemId);
        return ClientOrderItemMapper.toDTO(item);
    }

    @Override
    public void deleteItem(UUID itemId) {
        log.info("Eliminando item: {}", itemId);

        if (!clientOrderItemRepository.existsById(itemId)) {
            throw new ResourceNotFoundException("Item no encontrado con ID: " + itemId);
        }

        clientOrderItemRepository.deleteById(itemId);
        log.info("Item {} eliminado exitosamente", itemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientOrderItemDTO> getItemsByProduct(Long productId) {
        log.debug("Obteniendo items del producto: {}", productId);
        return clientOrderItemRepository.findByProductId(productId)
                .stream()
                .map(ClientOrderItemMapper::toDTO)
                .collect(Collectors.toList());
    }
}
