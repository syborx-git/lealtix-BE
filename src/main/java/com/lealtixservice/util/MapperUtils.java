package com.lealtixservice.util;

import com.lealtixservice.dto.AppUserDTO;
import com.lealtixservice.entity.AppUser;

public class MapperUtils {

    public static AppUserDTO toAppUserDTO(AppUser appUser) {
        if (appUser == null) {
            return null;
        }
        return AppUserDTO.builder()
                .id(appUser.getId())
                .fullName(appUser.getFullName())
                .email(appUser.getEmail())
                .telefono(appUser.getTelefono())
                .fechaNacimiento(appUser.getFechaNacimiento())
                .build();
    }
}
