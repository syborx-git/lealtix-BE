package com.lealtixservice.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class EncrypUtils {

    public static String encryptPassword(String password) {
        if (password == null) return null;
        return Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8));
    }

    public static String decrypPassword(String encrypted) {
        if (encrypted == null) return null;
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encrypted);
            return new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }
}
