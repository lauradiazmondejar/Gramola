package com.example.demo.web;

import java.util.Map;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Controlador de cuentas de bar (registro, login, confirmacion y reset).
 * Capa de presentacion: valida entrada HTTP y delega en servicios de dominio.
 */
@RestController
@RequestMapping("users")
@CrossOrigin(origins = {"http://127.0.0.1:4200"}) // Front solo en 127.0.0.1
public class UserController {

    @Autowired
    private UserService service;
    
    @PostMapping("/register")
    public void register(@RequestBody Map<String, Object> body) {
        // 1. Leemos todos los campos del JSON recibido desde el front.
        String email = (String) body.get("email");
        String pwd1 = (String) body.get("pwd1");
        String pwd2 = (String) body.get("pwd2");
        String bar = (String) body.get("bar");
        String clientId = (String) body.get("clientId");
        String clientSecret = (String) body.get("clientSecret");
        Double lat = body.get("lat") != null ? Double.valueOf(body.get("lat").toString()) : null;
        Double lon = body.get("lon") != null ? Double.valueOf(body.get("lon").toString()) : null;
        String signature = (String) body.get("signature");

        // 2. Validamos reglas basicas (las reglas de negocio reales viven en el servicio).
        if (pwd1 == null || !pwd1.equals(pwd2)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }
        if (pwd1.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters long");
        }
        if (email == null || !email.contains("@") || !email.contains(".")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is not valid");
        }
        if (signature == null || signature.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Signature is required");
        }
        // (Se pueden anadir validaciones extra para bar, clientId, etc.)

        // 3. Delegamos en el servicio para persistir y enviar correo de confirmacion.
        this.service.register(bar, email, pwd1, clientId, clientSecret, lat, lon, signature);
    }

    @DeleteMapping("/delete")
    public void delete(@RequestParam String email) {
        // Borra al usuario y sus datos asociados.
        this.service.delete(email);
    }

    @GetMapping("/confirmToken/{email}") 
    public void confirmToken(@PathVariable String email, @RequestParam String token, HttpServletResponse response) throws IOException {
        // Valida el token y redirige al flujo de pago en el front.
        this.service.confirmToken(email, token);
        response.sendRedirect("http://127.0.0.1:4200/payment?token=" + token);
    }

    // Devolvemos un JSON compacto con los datos necesarios para el OAuth de Spotify.
    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String pwd = body.get("password");

        if (email == null || pwd == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faltan credenciales");
        }
        
        com.example.demo.model.User user = this.service.authenticate(email, pwd); 

        // Construimos la respuesta JSON para el front.
        Map<String, String> response = new java.util.HashMap<>();
        response.put("clientId", user.getClientId());
        response.put("bar", user.getBar());
        
        // Enviamos la firma (si existe) para que el front la muestre.
        if (user.getSignature() != null) {
            response.put("signature", user.getSignature());
        } else {
            response.put("signature", ""); // O una imagen por defecto
        }

        return response;
    }

    @PostMapping("/reset/request")
    public void resetRequest(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta email");
        }
        // Genera token de reseteo y envia correo.
        this.service.requestPasswordReset(email);
    }

    @PostMapping("/reset/confirm")
    public void resetConfirm(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String pwd1 = body.get("pwd1");
        String pwd2 = body.get("pwd2");
        if (token == null || pwd1 == null || pwd2 == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faltan datos");
        }
        // Cambia la contrasena si el token es valido.
        this.service.resetPassword(token, pwd1, pwd2);
    }
}
