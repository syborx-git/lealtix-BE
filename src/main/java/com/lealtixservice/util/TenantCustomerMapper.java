package com.lealtixservice.util;

import com.lealtixservice.dto.TenantCustomerDTO;
import com.lealtixservice.entity.Allergy;
import com.lealtixservice.entity.Tenant;
import com.lealtixservice.entity.TenantCustomer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TenantCustomerMapper {

    public static TenantCustomerDTO toDTO(TenantCustomer entity) {
        if (entity == null) return null;
        List<String> allergies = entity.getAllergies() != null
                ? entity.getAllergies().stream().map(Allergy::getName).collect(Collectors.toList())
                : new ArrayList<>();
        return TenantCustomerDTO.builder()
                .id(entity.getId())
                .tenantId(entity.getTenant() != null ? entity.getTenant().getId() : null)
                .name(entity.getName())
                .email(entity.getEmail())
                .gender(entity.getGender())
                .birthDate(entity.getBirthDate())
                .phone(entity.getPhone())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .acceptedPromotions(entity.isAcceptedPromotions())
                .acceptedAt(entity.getAcceptedAt())
                .active(entity.getActive())
                .allergies(allergies)
                .build();
    }

    public static TenantCustomer toEntity(TenantCustomerDTO dto) {
        if (dto == null) return null;
        return TenantCustomer.builder()
                .id(dto.getId())
                .tenant(dto.getTenantId() != null ? Tenant.builder().id(dto.getTenantId()).build() : null)
                .name(dto.getName())
                .email(dto.getEmail())
                .gender(dto.getGender())
                .birthDate(dto.getBirthDate())
                .phone(dto.getPhone())
                .acceptedPromotions(dto.getAcceptedPromotions() != null ? dto.getAcceptedPromotions() : true)
                .acceptedAt(dto.getAcceptedAt())
                .active(true)
                .build();
    }
}
