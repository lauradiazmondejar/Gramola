package com.example.demo.web;

import java.util.Map;
import org.springframework.web.bind.annotation.CrossOrigin; // <-- AÑADIDO
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable; // <-- AÑADIDO
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

@RestController
@RequestMapping("users")
@CrossOrigin(origins = {"http://127.0.0.1:4200"}) // Front solo en 127.0.0.1
public class UserController {

    @Autowired
    private UserService service;
    
    @PostMapping("/register")
    public void register(@RequestBody Map<String, String> body) {
        // 1. Leemos todos los campos
        String email = body.get("email");
        String pwd1 = body.get("pwd1");
        String pwd2 = body.get("pwd2");
        String bar = body.get("bar"); // <-- NUEVO
        String clientId = body.get("clientId"); // <-- NUEVO
        String clientSecret = body.get("clientSecret"); // <-- NUEVO

        // 2. Validamos (tu código de validación estaba genial)
        if (pwd1 == null || !pwd1.equals(pwd2)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }
        if (pwd1.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters long");
        }
        if (email == null || !email.contains("@") || !email.contains(".")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is not valid");
        }
        // (Añade validaciones para bar, clientId, etc. si lo ves necesario)

        // 3. ¡LLAMAMOS AL SERVICIO! (Esto faltaba)
        this.service.register(bar, email, pwd1, clientId, clientSecret);
    }

    @DeleteMapping("/delete")
    public void delete(@RequestParam String email) {
        this.service.delete(email);
    }

    @GetMapping("/confirmToken/{email}") 
    public void confirmToken(@PathVariable String email, @RequestParam String token, HttpServletResponse response) throws IOException {
        this.service.confirmToken(email, token);
        response.sendRedirect("http://127.0.0.1:4200/payment?token=" + token);
    }

    @PostMapping("/login")
    public String login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String pwd = body.get("password");

        if (email == null || pwd == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faltan credenciales");
        }

        return this.service.login(email, pwd);
    }
}
