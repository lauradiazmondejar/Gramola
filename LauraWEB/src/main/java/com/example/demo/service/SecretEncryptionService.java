package com.example.demo.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class SecretEncryptionService {

    @Value("${app.crypto.key:change-me-please-change-me-32bytes}")
    private String secretKeyConfig;

    private javax.crypto.spec.SecretKeySpec keySpec;
    private final SecureRandom random = new SecureRandom();

    @PostConstruct
    public void init() {
        // Ensure 32 bytes (AES-256). If provided key is shorter, pad with zeros.
        byte[] keyBytes = new byte[32];
        byte[] src = secretKeyConfig.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(src, 0, keyBytes, 0, Math.min(src.length, keyBytes.length));
        this.keySpec = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plain) {
        if (plain == null || plain.isBlank()) {
            return plain;
        }
        try {
            byte[] iv = new byte[12];
            random.nextBytes(iv);

            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
            javax.crypto.spec.GCMParameterSpec spec = new javax.crypto.spec.GCMParameterSpec(128, iv);
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keySpec, spec);

            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            // In case of error, return original to avoid blocking registration; log could be added.
            return plain;
        }
    }

    public String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) {
            return encrypted;
        }
        try {
            byte[] data = Base64.getDecoder().decode(encrypted);
            if (data.length < 13) {
                return encrypted; // Not a valid payload
            }
            byte[] iv = new byte[12];
            byte[] cipherText = new byte[data.length - 12];
            System.arraycopy(data, 0, iv, 0, 12);
            System.arraycopy(data, 12, cipherText, 0, cipherText.length);

            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
            javax.crypto.spec.GCMParameterSpec spec = new javax.crypto.spec.GCMParameterSpec(128, iv);
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec, spec);

            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Si hay datos previos en texto plano o clave cambiada, devolvemos tal cual.
            return encrypted;
        }
    }
}
