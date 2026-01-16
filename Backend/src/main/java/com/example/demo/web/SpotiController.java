package com.example.demo.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.SpotiToken;
import com.example.demo.service.SpotiService;

/**
 * API puente con Spotify para el intercambio de code -> access token.
 */
@RestController
@RequestMapping("spoti")
@CrossOrigin(origins = {"http://127.0.0.1:4200"}) // Front solo en 127.0.0.1
public class SpotiController {

    @Autowired
    private SpotiService spotiService;

    @GetMapping("/getAuthorizationToken")
    public SpotiToken getAuthorizationToken(@RequestParam String code, @RequestParam String clientId) {
        // Intercambia el codigo de Spotify por un token de acceso/refresh.
        return spotiService.getAuthorizationToken(code, clientId);
    }
}
