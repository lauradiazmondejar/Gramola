package com.example.demo;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.demo.dao.PriceDao;
import com.example.demo.dao.UserDao;
import com.example.demo.model.Price;
import com.example.demo.model.User;
import com.example.demo.service.PaymentService;
import com.example.demo.service.UserService;

import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * E2E UI para pago de suscripcion via Stripe test cards.
 * Requiere: front sirviendo en 127.0.0.1:4200 y Stripe secreto configurado.
 * Habilitar con: -DrunUiStripeTests=true
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = { "server.port=8080" })
@ActiveProfiles("test")
class UiSubscriptionSeleniumTests {

    @Value("${e2e.base-url:http://127.0.0.1:4200}")
    private String baseUrl;

    @Autowired
    private UserService userService;
    @Autowired
    private UserDao userDao;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private PriceDao priceDao;

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setup() {
        assumeTrue(Boolean.parseBoolean(System.getProperty("runUiStripeTests", "false")),
                "UI Stripe tests desactivados (añade -DrunUiStripeTests=true)");

        // Precios requeridos en BD
        seedPrice("subscription_monthly", "Suscripcion mensual", 1000L);

        // Limpieza de usuarios
        userDao.deleteAll();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu", "--remote-allow-origins=*");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    @AfterEach
    void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void pagoSuscripcionOkMarcaPagado() throws Exception {
        String email = "ui-stripe-ok@test.com";
        String token = userService.register("Bar UI OK", email, "12345678", "client-id", "client-secret", null, null, null);

        abrirPaginaPago(token);
        iniciarPago();
        rellenarTarjeta("4242424242424242", "12/34", "123");
        confirmarPagoEnStripe();

        // Esperar mensaje de éxito y redirección a login
        wait.until(ExpectedConditions.urlContains("/login"));

        User saved = userDao.findById(email).orElseThrow();
        assertTrue(saved.isPaid(), "El usuario debe quedar pagado tras el flujo UI");
    }

    @Test
    void pagoSuscripcionKoNoMarcaPagado() throws Exception {
        String email = "ui-stripe-ko@test.com";
        String token = userService.register("Bar UI KO", email, "12345678", "client-id", "client-secret", null, null, null);

        abrirPaginaPago(token);
        iniciarPago();
        rellenarTarjeta("4000000000000002", "12/34", "123"); // tarjeta de rechazo
        confirmarPagoEnStripe();

        // Debe mostrarse un mensaje de error y no redirigir
        // Damos unos segundos para que aparezca feedback
        Thread.sleep(3000);
        User saved = userDao.findById(email).orElseThrow();
        assertFalse(saved.isPaid(), "El usuario no debe quedar pagado si Stripe rechaza");
    }

    // Helpers
    private void abrirPaginaPago(String token) {
        driver.get(baseUrl + "/payment?token=" + token);
    }

    private void iniciarPago() {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button.btn")));
        btn.click();
    }

    private void rellenarTarjeta(String num, String exp, String cvc) {
        // Stripe Elements usa iframes; buscamos el primero
        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.cssSelector("iframe[name^='__privateStripeFrame']")));
        List<WebElement> inputs = wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.cssSelector("input[name]"), 0));
        // Orden típico: cardnumber, exp-date, cvc, postal
        inputs.get(0).sendKeys(num);
        inputs.get(1).sendKeys(exp);
        inputs.get(2).sendKeys(cvc);
        driver.switchTo().defaultContent();
    }

    private void confirmarPagoEnStripe() {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("#payment-form button.btn")));
        btn.click();
    }

    private void seedPrice(String code, String desc, long amount) {
        priceDao.findById(code).ifPresentOrElse(p -> {}, () -> {
            Price price = new Price();
            price.setCode(code);
            price.setDescription(desc);
            price.setAmount(amount);
            priceDao.save(price);
        });
    }
}
