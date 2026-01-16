package com.example.demo;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dao.StripeTransactionDao;
import com.example.demo.dao.UserDao;
import com.example.demo.dao.PriceDao;
import com.example.demo.model.Price;
import com.example.demo.model.User;
import com.example.demo.service.PaymentService;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentConfirmParams;

/**
 * Flujos de suscripcion con Stripe (solo se ejecutan si se habilita runStripeTests=true y hay clave Stripe).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentFlowIntegrationTests {

    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserDao userDao;

    @Autowired
    private StripeTransactionDao stripeDao;

    @Autowired
    private PriceDao priceDao;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void clean() {
        // Solo ejecutamos si estan habilitados y hay clave Stripe.
        boolean runStripe = Boolean.parseBoolean(System.getProperty("runStripeTests", "false"));
        assumeTrue(runStripe, "Tests de Stripe desactivados (añade -DrunStripeTests=true para ejecutarlos)");
        assumeTrue(stripeSecretKey != null && !stripeSecretKey.isBlank(), "Configura stripe.secret-key para ejecutar los tests de Stripe");

        stripeDao.deleteAll();
        userDao.deleteAll();
        priceDao.deleteAll();

        // Insertamos precios necesarios en BD (enunciado: precios en BD, no hardcode).
        Price price = new Price();
        price.setCode("subscription_monthly");
        price.setDescription("Suscripcion mensual");
        price.setAmount(1000L);
        priceDao.save(price);
    }

    @Test
    void suscripcionPagoOkMarcaUsuarioComoPagado() throws Exception {
        String email = "stripe-ok@test.com";
        String token = userService.register("Bar Stripe OK", email, "12345678", "client-id", "client-secret", null, null, null);

        String prepayJson = paymentService.prepay("subscription_monthly", email, "Bar Stripe OK", "subscription");
        JsonNode node = mapper.readTree(prepayJson);
        String stripeId = node.get("id").asText();

        // Confirmar en Stripe con tarjeta de prueba OK.
        PaymentIntent intent = PaymentIntent.retrieve(stripeId);
        PaymentIntentConfirmParams confirmParams = PaymentIntentConfirmParams.builder()
                .setPaymentMethod("pm_card_visa")
                .build();
        intent.confirm(confirmParams);

        paymentService.confirm(stripeId, stripeId, token);

        User saved = userDao.findById(email).orElseThrow();
        assertEquals(true, saved.isPaid(), "El usuario debe quedar marcado como pagado tras confirmar Stripe");
    }

    @Test
    void suscripcionPagoFallidoNoMarcaPagado() throws Exception {
        String email = "stripe-ko@test.com";
        String token = userService.register("Bar Stripe KO", email, "12345678", "client-id", "client-secret", null, null, null);

        String prepayJson = paymentService.prepay("subscription_monthly", email, "Bar Stripe KO", "subscription");
        JsonNode node = mapper.readTree(prepayJson);
        String stripeId = node.get("id").asText();

        // No se confirma el PaymentIntent (estatus seguira en requires_payment_method).
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                paymentService.confirm(stripeId, stripeId, token));
        assertEquals(400, ex.getStatusCode().value());

        User saved = userDao.findById(email).orElseThrow();
        assertFalse(saved.isPaid(), "El usuario no debe quedar pagado si Stripe no confirma");
    }
}

