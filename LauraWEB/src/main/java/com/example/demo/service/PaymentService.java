package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

    @Autowired
    private PriceDao priceDao;

    @PostConstruct
    public void ensureDefaultPrices() {
        // Cargamos precios por defecto si no existen
        createIfMissing("subscription_monthly", "Suscripcion mensual", 1000L);
        createIfMissing("subscription_annual", "Suscripcion anual", 10000L);
        createIfMissing("song", "Precio por cancion", 50L);
    }

    private void createIfMissing(String code, String description, long amount) {
        priceDao.findById(code).ifPresentOrElse(
                p -> {},
                () -> {
                    Price price = new Price();
                    price.setCode(code);
                    price.setDescription(description);
                    price.setAmount(amount);
                    priceDao.save(price);
                });
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
        } else if ("song".equals(transaction.getPriceCode())) {
            // Pago de cancion: ya validado en Stripe, no necesita token.
        }
    }
}
