package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.util.Locale;

/**
 * Encapsula el envio de correos del flujo de negocio:
 * registro/confirmacion, reset y recibos de suscripcion.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.frontend.host:http://127.0.0.1:4200}")
    private String frontendHost;

    @Value("${spring.mail.username:}")
    private String from;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendRegistrationEmail(String to, String token) {
        // Construye enlaces de confirmacion y pago para nuevos registros.
        String confirmUrl = "http://localhost:8080/users/confirmToken/" + to + "?token=" + token;
        String paymentUrl = frontendHost + "/payment?token=" + token;

        String body = """
                Bienvenido a la gramola.

                Confirma tu cuenta pulsando en este enlace:
                %s

                Tras confirmar, se abrirá la pasarela de pago:
                %s
                """.formatted(confirmUrl, paymentUrl);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(from.isBlank() ? "no-reply@gramola.local" : from);
        message.setSubject("Confirma tu cuenta en la gramola");
        message.setText(body);

        try {
            mailSender.send(message);
            log.info("Correo de registro enviado a {}", to);
        } catch (Exception e) {
            log.error("No se pudo enviar el correo a {}: {}", to, e.getMessage());
            throw e;
        }
    }

    public void sendResetEmail(String to, String token) {
        // Genera correo con enlace para restablecer contrasena.
        String resetUrl = frontendHost + "/reset?token=" + token;

        String body = """
                Has solicitado restablecer tu contraseña en la gramola.

                Pulsa este enlace para elegir una nueva contraseña:
                %s
                """.formatted(resetUrl);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(from.isBlank() ? "no-reply@gramola.local" : from);
        message.setSubject("Restablecer contraseña");
        message.setText(body);

        try {
            mailSender.send(message);
            log.info("Correo de reset enviado a {}", to);
        } catch (Exception e) {
            log.error("No se pudo enviar el correo de reset a {}: {}", to, e.getMessage());
            throw e;
        }
    }

    public void sendSubscriptionReceipt(String to, String bar, String planCode, Long amountCents) {
        // Resume el pago de suscripcion y lo envia al correo del bar.
        String planName = switch (planCode) {
            case "subscription_monthly" -> "Suscripcion mensual";
            case "subscription_annual" -> "Suscripcion anual";
            default -> planCode;
        };
        double amountEuros = amountCents != null ? amountCents / 100.0 : 0.0;

        String body = """
                Hola %s,

                Hemos recibido el pago de tu suscripcion (%s).
                Importe pagado: %.2f euros.

                Gracias por usar la gramola.
                """.formatted(bar == null ? "" : bar, planName, amountEuros);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(from.isBlank() ? "no-reply@gramola.local" : from);
        message.setSubject("Pago confirmado de la suscripcion");
        message.setText(body);

        try {
            mailSender.send(message);
            log.info("Correo de confirmacion de pago enviado a {}", to);
        } catch (Exception e) {
            log.error("No se pudo enviar el correo de pago a {}: {}", to, e.getMessage());
            throw e;
        }
    }
}
