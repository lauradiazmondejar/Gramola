package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;

import com.example.demo.dao.StripeTransactionDao;
import com.example.demo.dao.TokenDao;
import com.example.demo.dao.UserDao;
import com.example.demo.dao.PriceDao;
import com.example.demo.model.Price;
import com.example.demo.model.StripeTransaction;
import com.example.demo.model.Token;
import com.example.demo.model.User;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams.AutomaticPaymentMethods;
import com.stripe.param.PaymentIntentCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;

    @Autowired
    private StripeTransactionDao stripeDao;

    @Autowired
    private TokenDao tokenDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private PriceDao priceDao;

    @Autowired
    private EmailService emailService;

    @PostConstruct
    public void initStripe() {
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            log.warn("Stripe secret key is not configured (stripe.secret-key). Payments will fail.");
        } else {
            Stripe.apiKey = stripeSecretKey;
        }
    }

    public Price getPrice(String code) {
        return priceDao.findById(code)
                .orElseThrow(() -> new IllegalArgumentException("Price code not found: " + code));
    }

    public java.util.List<Price> listPrices() {
        return priceDao.findAll();
    }

    public boolean hasValidSongPayment(String email) {
        return stripeDao.findFirstByEmailAndPriceCodeAndUsedFalse(email, "song").isPresent();
    }

    public StripeTransaction consumeSongPayment(String email) {
        StripeTransaction tx = stripeDao.findFirstByEmailAndPriceCodeAndUsedFalse(email, "song")
                .orElseThrow(() -> new IllegalArgumentException("No hay pago de canción disponible para " + email));
        tx.setUsed(true);
        stripeDao.save(tx);
        return tx;
    }

    public String prepay(String priceCode, String email, String bar, String type) throws StripeException {
        Price price = getPrice(priceCode);

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setCurrency("eur")
                .setAmount(price.getAmount())
                .setAutomaticPaymentMethods(
                        AutomaticPaymentMethods.builder().setEnabled(true).build()
                )
                .build();

        PaymentIntent intent = PaymentIntent.create(params);

        StripeTransaction transaction = new StripeTransaction();
        transaction.setId(intent.getId());
        transaction.setData(intent.toJson());
        transaction.setAmount(price.getAmount());
        transaction.setPriceCode(priceCode);
        transaction.setEmail(email);
        transaction.setBar(bar);
        transaction.setType(type);
        stripeDao.save(transaction);

        return intent.toJson();
    }

    public void confirm(String stripeId, String internalId, String token) throws StripeException {
        if (stripeId == null || internalId == null) {
            throw new IllegalArgumentException("Missing payment confirmation data");
        }

        // Asegurarnos de que el pago que vuelve es el mismo que preparamos
        StripeTransaction transaction = stripeDao.findById(internalId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not registered"));
        if (!stripeId.equals(transaction.getId())) {
            throw new IllegalArgumentException("Stripe identifiers do not match");
        }

        // Verificar en Stripe que el pago termino OK
        PaymentIntent intent = PaymentIntent.retrieve(stripeId);
        if (!"succeeded".equalsIgnoreCase(intent.getStatus())) {
            throw new IllegalArgumentException("Payment not completed in Stripe");
        }

        if (transaction.getAmount() != null && intent.getAmount() != null
                && !transaction.getAmount().equals(intent.getAmount())) {
            throw new IllegalArgumentException("Payment amount mismatch");
        }

        // Si es suscripcion: token obligatorio y marcamos pagado
        if ("subscription_monthly".equals(transaction.getPriceCode()) || "subscription_annual".equals(transaction.getPriceCode())) {
            if (token == null) {
                throw new IllegalArgumentException("Missing token for subscription confirmation");
            }
            User user = userDao.findByCreationToken_Id(token)
                    .orElseThrow(() -> new IllegalArgumentException("User not found for token"));
            Token creationToken = user.getCreationToken();
            if (creationToken == null) {
                throw new IllegalArgumentException("User does not have a creation token");
            }

            if (!creationToken.isUsed()) {
                creationToken.use();
                tokenDao.save(creationToken);
            }

            user.setPaid(true);
            userDao.save(user);
            try {
                emailService.sendSubscriptionReceipt(user.getEmail(), user.getBar(), transaction.getPriceCode(), transaction.getAmount());
            } catch (Exception e) {
                // El pago se confirma aunque el correo falle, pero dejamos logueado el error.
                log.error("No se pudo enviar el correo de confirmacion: {}", e.getMessage());
            }
        } else if ("song".equals(transaction.getPriceCode())) {
            // Pago de cancion: ya validado en Stripe, no necesita token.
        }
    }
}
