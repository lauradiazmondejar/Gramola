package com.example.demo.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.example.demo.model.SpotiToken;
import com.example.demo.model.User;

/**
 * Encapsula el intercambio OAuth con Spotify (authorization code).
 */
@Service
public class SpotiService {

    @Autowired
    private UserService userService;

    @Autowired
    private SecretEncryptionService encryptionService;

    public SpotiToken getAuthorizationToken(String code, String clientId) {
        // 1. Recuperamos el secreto del bar de la BD.
        User user = userService.getUserByClientId(clientId);
        String clientSecret = encryptionService.decrypt(user.getClientSecret());

        // 2. Preparamos la peticion a Spotify.
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", basicAuth(clientId, clientSecret));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("grant_type", "authorization_code");
        body.add("redirect_uri", "http://127.0.0.1:4200/callback"); // Debe coincidir EXACTO con Spotify.

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        RestTemplate restTemplate = new RestTemplate();

        // 3. Hacemos la llamada.
        ResponseEntity<SpotiToken> response = restTemplate.postForEntity(
                "https://accounts.spotify.com/api/token",
                request,
                SpotiToken.class);

        return response.getBody();
    }

    private String basicAuth(String clientId, String clientSecret) {
        // Construye el header Basic Auth requerido por Spotify.
        String auth = clientId + ":" + clientSecret;
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    }
}
