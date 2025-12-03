package com.example.demo.service;

import java.util.Optional;

import org.apache.commons.codec.digest.DigestUtils; // <--- NUEVO IMPORT
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dao.TokenDao;
import com.example.demo.dao.UserDao;
import com.example.demo.model.Token;
import com.example.demo.model.User;

@Service
public class UserService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private TokenDao tokenDao;

    public String register(String bar, String email, String pwd, String clientId, String clientSecret) {
        Optional<User> optUser = userDao.findById(email);
        
        if (optUser.isPresent()) {
            User existingUser = optUser.get();
            // LÓGICA DE "LIMPIEZA":
            // Si el usuario existe pero su token NO ha sido usado (no está confirmado)...
            if (existingUser.getCreationToken() != null && !existingUser.getCreationToken().isUsed()) {
                // ... lo borramos para permitir un nuevo registro limpio.
                userDao.delete(existingUser);
                // Forzamos que la base de datos ejecute el borrado ya
                userDao.flush(); 
            } else {
                // Si ya estaba confirmado, entonces sí es un conflicto real.
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya existe y está confirmado");
            }
        }

        User user = new User();
        user.setEmail(email);
        
        // ENCRIPTACIÓN DE CONTRASEÑA (SHA-512)
        user.setPassword(DigestUtils.sha512Hex(pwd)); 
        
        user.setBar(bar);
        user.setClientId(clientId);
        user.setClientSecret(clientSecret);
        user.setCreationToken(new Token());

        userDao.save(user);

        // Email simulado
        System.out.println("--- ENVIAR EMAIL (SIMULADO) ---");
        System.out.println("http://localhost:8080/users/confirmToken/" + email + "?token=" + user.getCreationToken().getId());

        return user.getCreationToken().getId();
    }

    public void confirmToken(String email, String token) {
        User user = userDao.findById(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Token userToken = user.getCreationToken();
        if (userToken == null) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Token not found for user");
        }

        if (!userToken.getId().equals(token)) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Token does not match");
        }

        if (userToken.getCreationTime() < System.currentTimeMillis() - 300000) {
            tokenDao.delete(userToken);
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Token expired");
        }

        if (userToken.isUsed()) {
            throw new ResponseStatusException(HttpStatus.GONE, "Token already used");
        }

        userToken.use();
        tokenDao.save(userToken); 
    }

    public void delete(String email) {
        userDao.deleteById(email);
    }

    public String login(String email, String pwd) {
        // 1. Buscar al usuario
        User user = userDao.findById(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        // 2. Verificar contraseña (encriptándola primero para comparar)
        String pwdEncrypted = DigestUtils.sha512Hex(pwd);
        if (!user.getPassword().equals(pwdEncrypted)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Contraseña incorrecta");
        }

        // 3. Devolver el Client ID de Spotify
        // Este dato es necesario para que el front inicie el protocolo OAuth2 con Spotify
        return user.getClientId();
    }

    public com.example.demo.model.User getUserByClientId(String clientId) {
        return userDao.findFirstByClientId(clientId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bar no encontrado para este Client ID"));
    }
}
