package com.example.demo.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import com.example.demo.service.PaymentService;

@RestController
@RequestMapping("payments")
@CrossOrigin(origins = {"http://127.0.0.1:4200"})
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/prepay")
    public String prepay(@RequestParam long amount) {
        try {
            // Llamamos al servicio para iniciar el pago
            return paymentService.prepay(amount);
        } catch (Exception e) {
            // Si Stripe falla (clave mal, error de conexion), devolvemos error
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/confirm")
    public String confirm(@RequestBody Map<String, Object> body) {
        try {
            String stripeId = (String) body.get("stripeId");
            String internalId = (String) body.get("internalId");
            String token = (String) body.get("token");

            paymentService.confirm(stripeId, internalId, token);
            return "OK";
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
