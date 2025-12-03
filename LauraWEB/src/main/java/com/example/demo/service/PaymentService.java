package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dao.StripeTransactionDao;
import com.example.demo.dao.TokenDao;
import com.example.demo.dao.UserDao;
import com.example.demo.model.StripeTransaction;
import com.example.demo.model.Token;
import com.example.demo.model.User;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

@Service
public class PaymentService {

    // Clave secreta de prueba. Sustituye por la tuya (sk_test_...) si usas otra cuenta.
    static {
        Stripe.apiKey = "sk_test_51SIV2CRfAGkgoJHtxiS8AsEUX3c4f6zANAokuFi9kRECp1EUt2LAVig0AWgR5fshnIZefP5KTSZkpWY0vIZlPDaK003osgReMe";
    }

    @Autowired
    private StripeTransactionDao stripeDao;

    @Autowired
    private TokenDao tokenDao;

    @Autowired
    private UserDao userDao;

    public String prepay(long amount) throws StripeException {
        // 1. Preparar la peticion a Stripe (cantidad en centimos y moneda)
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setCurrency("eur")
                .setAmount(amount)
                .build();

        // 2. Enviar la peticion a Stripe
        PaymentIntent intent = PaymentIntent.create(params);

        // 3. Guardar el resultado en la base de datos
        StripeTransaction transaction = new StripeTransaction();
        transaction.setId(intent.getId());
        transaction.setData(intent.toJson());
        stripeDao.save(transaction);

        // 4. Devolver el PaymentIntent como JSON (contiene el client_secret)
        return intent.toJson();
    }

    public void confirm(String stripeId, String internalId, String token) throws StripeException {
        if (stripeId == null || internalId == null || token == null) {
            throw new IllegalArgumentException("Missing payment confirmation data");
        }

        // Asegurarnos de que el pago que vuelve es el mismo que preparamos
        stripeDao.findById(internalId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not registered"));
        if (!stripeId.equals(internalId)) {
            throw new IllegalArgumentException("Stripe identifiers do not match");
        }

        // Verificar en Stripe que el pago termino OK
        PaymentIntent intent = PaymentIntent.retrieve(stripeId);
        if (!"succeeded".equalsIgnoreCase(intent.getStatus())) {
            throw new IllegalArgumentException("Payment not completed in Stripe");
        }

        // Vincular el pago con el usuario mediante el token
        User user = userDao.findByCreationToken_Id(token)
                .orElseThrow(() -> new IllegalArgumentException("User not found for token"));
        Token creationToken = user.getCreationToken();
        if (creationToken == null) {
            throw new IllegalArgumentException("User does not have a creation token");
        }

        // Si alguien accede directo al pago sin pasar por confirmacion de correo
        if (!creationToken.isUsed()) {
            creationToken.use();
            tokenDao.save(creationToken);
        }
    }
}
