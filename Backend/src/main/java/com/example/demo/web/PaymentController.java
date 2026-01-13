package com.example.demo.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    public String prepay(@RequestParam String code, @RequestParam(required = false) String email,
            @RequestParam(required = false) String bar, @RequestParam(defaultValue = "subscription") String type) {
        try {
            // Llama al servicio para preparar el pago y devolver el intent de Stripe
            return paymentService.prepay(code, email, bar, type);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/confirm")
    public String confirm(@RequestBody Map<String, Object> body) {
        try {
            // Extrae datos enviados por el front al confirmar el pago
            String stripeId = (String) body.get("stripeId");
            String internalId = (String) body.get("internalId");
            String token = (String) body.get("token"); // puede ser null para pagos de cancion

            // Marca el pago como confirmado en base de datos
            paymentService.confirm(stripeId, internalId, token);
            return "OK";
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/prices")
    public java.util.List<com.example.demo.model.Price> listPrices() {
        return paymentService.listPrices();
    }

    @GetMapping("/prices/{code}")
    public com.example.demo.model.Price getPrice(@PathVariable String code) {
        try {
            // Recupera un precio especifico para mostrarlo en el front
            return paymentService.getPrice(code);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
