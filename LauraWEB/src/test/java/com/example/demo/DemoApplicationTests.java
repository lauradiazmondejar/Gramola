package com.example.demo;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.dao.SongDao;
import com.example.demo.dao.StripeTransactionDao;
import com.example.demo.dao.UserDao;
import com.example.demo.model.Song;
import com.example.demo.model.StripeTransaction;
import com.example.demo.model.User;

import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * E2E Selenium + Stripe mock.
 * Ejecutar con: mvn test -DrunSeleniumTests=true (front en 127.0.0.1:4200, backend en 8080).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class DemoApplicationTests {

    @Value("${e2e.base-url:http://127.0.0.1:4200}")
    private String baseUrl;

    @Autowired
    private UserDao userDao;
    @Autowired
    private SongDao songDao;
    @Autowired
    private StripeTransactionDao stripeDao;

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setup() {
        assumeTrue(Boolean.parseBoolean(System.getProperty("runSeleniumTests", "false")),
                "Selenium E2E desactivado (añade -DrunSeleniumTests=true para ejecutarlo)");

        // Verificar que el front está arriba antes de lanzar Selenium
        assumeTrue(frontendUp(), "Front no disponible en " + baseUrl);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu", "--remote-allow-origins=*");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Limpieza y datos base
        songDao.deleteAll();
        stripeDao.deleteAll();
        userDao.deleteAll();

        User user = new User();
        user.setEmail("test@selenium.com");
        user.setBar("Bar Selenium");
        user.setPassword("12345678");
        user.setClientId("fake-client-id");
        user.setClientSecret("fake-client-secret");
        userDao.save(user);
    }

    @AfterEach
    void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void pagoOkYGuardaCancion() throws Exception {
        abrirPantallaPagoConMocks(true);

        // Simulamos persistencia de la transacción tras pago OK
        StripeTransaction tx = new StripeTransaction();
        tx.setId("pi_test_ok");
        tx.setData("{}");
        stripeDao.save(tx);

        // Lanzamos la llamada real al backend /music/add desde el navegador (equivale a "poner" canción)
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Object res = js.executeAsyncScript("""
            const done = arguments[0];
            fetch('http://127.0.0.1:8080/music/add', {
              method:'POST',
              headers:{'Content-Type':'application/json'},
              body: JSON.stringify({title:'Test Song', artist:'Tester', uri:'spotify:track:test', clientId:'fake-client-id'})
            }).then(() => done('ok')).catch(e => done('err:'+e));
        """);
        assertEquals("ok", res, "La llamada a /music/add debe completarse");

        assertEquals(1, stripeDao.count(), "Debe existir una transacción de pago");
        assertEquals(1, songDao.count(), "Debe haberse guardado la canción");
    }

    @Test
    void pagoFallaNoGuardaCancion() throws Exception {
        abrirPantallaPagoConMocks(false);

        // Sin pago OK no debe haber registros
        assertEquals(0, stripeDao.count(), "No debe haber transacción al fallar el pago");
        assertEquals(0, songDao.count(), "No debe guardarse canción al fallar el pago");
    }

    private void abrirPantallaPagoConMocks(boolean pagoOk) {
        driver.get(baseUrl + "/payment?token=test-token");
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Mock de fetch para pagos
        js.executeScript("""
            (function() {
              const ok = arguments[0];
              const originalFetch = window.fetch.bind(window);
              window.fetch = (url, opts) => {
                if (url.includes('/payments/prepay')) {
                  const body = '{"id":"pi_test","client_secret":"cs_test"}';
                  return Promise.resolve(new Response(body, {status:200, headers:{'Content-Type':'application/json'}}));
                }
                if (url.includes('/payments/confirm')) {
                  if (ok) {
                    return Promise.resolve(new Response('OK', {status:200, headers:{'Content-Type':'text/plain'}}));
                  }
                  return Promise.resolve(new Response('FAIL', {status:400, headers:{'Content-Type':'text/plain'}}));
                }
                return originalFetch(url, opts);
              };
            })(arguments[0]);
        """, pagoOk);

        // Mock de Stripe JS
        js.executeScript("""
            window.Stripe = function() {
              return {
                elements() { return { create() { return { mount(){}, destroy(){}, addEventListener(){} }; } }; },
                confirmCardPayment() {
                  if (arguments[0] && arguments[0].includes('cs_test')) {
                    return Promise.resolve({ paymentIntent: { status: 'succeeded' } });
                  }
                  return Promise.reject({ error: { message:'fail' } });
                }
              };
            };
        """);

        WebElement pagarBtn = wait.until(
                ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(.,'Pagar')]")));
        pagarBtn.click();

        WebElement confirmarBtn = wait.until(
                ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(.,'Confirmar')]")));
        confirmarBtn.click();
    }

    private boolean frontendUp() {
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(baseUrl + "/").openConnection();
            con.setConnectTimeout(2000);
            con.setReadTimeout(2000);
            con.setRequestMethod("GET");
            int code = con.getResponseCode();
            return code >= 200 && code < 500;
        } catch (Exception e) {
            return false;
        }
    }
}
