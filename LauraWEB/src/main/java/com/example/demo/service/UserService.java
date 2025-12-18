package com.example.demo.service;

import java.util.Optional;

import org.apache.commons.codec.digest.DigestUtils;
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

    @Autowired
    private EmailService emailService;

    public String register(String bar, String email, String pwd, String clientId, String clientSecret, Double lat, Double lon, String signature) {
        Optional<User> optUser = userDao.findById(email);

        if (optUser.isPresent()) {
            User existingUser = optUser.get();
            boolean tokenPending = existingUser.getCreationToken() != null && !existingUser.getCreationToken().isUsed();
            boolean unpaid = !existingUser.isPaid();

            if (tokenPending || unpaid) {
                // Permitimos reintentar registro si no confirmo o si confirmo pero no pago.
                userDao.delete(existingUser);
                userDao.flush();
            } else {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya existe y esta confirmado");
            }
        }

        User user = new User();
        user.setEmail(email);

        // Encriptamos con SHA-512
        user.setPassword(DigestUtils.sha512Hex(pwd));

        user.setBar(bar);
        user.setClientId(clientId);
        user.setClientSecret(clientSecret);
        user.setCreationToken(new Token());
        user.setLatitude(lat);
        user.setLongitude(lon);
        user.setSignature(signature);
        user.setPaid(false);

        userDao.save(user);

        emailService.sendRegistrationEmail(email, user.getCreationToken().getId());

        return user.getCreationToken().getId();
    }

    public void requestPasswordReset(String email) {
        User user = userDao.findById(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Token token = new Token();
        user.setResetToken(token);
        userDao.save(user);

        emailService.sendResetEmail(email, token.getId());
    }

    public void resetPassword(String token, String pwd1, String pwd2) {
        if (pwd1 == null || pwd2 == null || !pwd1.equals(pwd2)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }
        if (pwd1.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters long");
        }

        User user = userDao.findByResetToken_Id(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Token no valido"));

        Token reset = user.getResetToken();
        if (reset == null) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "No hay token de reset");
        }
        if (reset.getCreationTime() < System.currentTimeMillis() - 300000) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Token expirado");
        }

        user.setPassword(DigestUtils.sha512Hex(pwd1));
        user.setResetToken(null);
        userDao.save(user);
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
        User user = userDao.findById(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        String pwdEncrypted = DigestUtils.sha512Hex(pwd);
        if (!user.getPassword().equals(pwdEncrypted)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Contrasena incorrecta");
        }

        if (!user.isPaid()) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Cuenta pendiente de pago");
        }

        return user.getClientId();
    }

    public User getUserByClientId(String clientId) {
        return userDao.findFirstByClientId(clientId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bar no encontrado para este Client ID"));
    }

    public User authenticate(String email, String pwd) {
        User user = userDao.findById(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        String pwdEncrypted = DigestUtils.sha512Hex(pwd);
        if (!user.getPassword().equals(pwdEncrypted)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Contrasena incorrecta");
        }

        if (!user.isPaid()) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Cuenta pendiente de pago");
        }

        return user;
    }
}
