package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Valida la presencia de variables críticas y avisa en consola si faltan.
 */
@Component
public class StartupConfigValidator {

    private static final Logger log = LoggerFactory.getLogger(StartupConfigValidator.class);

    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${MAILTRAP_USER:}")
    private String mailUser;

    @Value("${MAILTRAP_PASS:}")
    private String mailPass;

    @Value("${app.crypto.key:change-me-please-change-me-32bytes}")
    private String cryptoKey;

    @PostConstruct
    public void checkConfig() {
        // Comprueba en arranque que las variables sensibles esten presentes
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            log.error("Falta la clave secreta de Stripe (stripe.secret-key / STRIPE_SECRET_KEY). Los pagos fallarán.");
        }
        if (mailUser == null || mailUser.isBlank() || mailPass == null || mailPass.isBlank()) {
            log.error("Faltan credenciales SMTP (MAILTRAP_USER / MAILTRAP_PASS). No se enviarán correos.");
        }
        if (cryptoKey == null || cryptoKey.isBlank() || cryptoKey.startsWith("change-me")) {
            log.warn("La clave de cifrado (app.crypto.key / APP_CRYPTO_KEY) es la de ejemplo. Cámbiala en despliegue.");
        }
    }
}
