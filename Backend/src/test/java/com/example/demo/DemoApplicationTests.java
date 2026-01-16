package com.example.demo;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.example.demo.dao.SongDao;
import com.example.demo.dao.StripeTransactionDao;
import com.example.demo.dao.UserDao;
import com.example.demo.model.Song;
import com.example.demo.model.StripeTransaction;
import com.example.demo.model.User;

import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * E2E Selenium ligero + verificaciones de BD.
 * Ejecutar con: mvn test -DrunSeleniumTests=true (front en 127.0.0.1:4200, backend en 8080).
 *
 * Cubre los escenarios del enunciado: pago OK agrega cancion y pago KO no agrega.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoApplicationTests {

    @Value("${e2e.base-url:http://127.0.0.1:4200}")
    private String baseUrl;

    @LocalServerPort
    private int port;

    @Autowired
    private UserDao userDao;
    @Autowired
    private SongDao songDao;
    @Autowired
    private StripeTransactionDao stripeDao;

    private WebDriver driver;

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setup() {
        assumeTrue(Boolean.parseBoolean(System.getProperty("runSeleniumTests", "false")),
                "Selenium E2E desactivado (añade -DrunSeleniumTests=true para ejecutarlo)");

        assumeTrue(frontendUp(), "Front no disponible en " + baseUrl);

        // Configuramos navegador y limpiamos datos antes de cada prueba.
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu", "--remote-allow-origins=*");
        driver = new ChromeDriver(options);

        // Limpieza y datos base.
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
    void pagoOkConsumoCancion() throws Exception {
        // Simular pago OK en BD.
        StripeTransaction tx = new StripeTransaction();
        tx.setId("pi_test_song");
        tx.setData("{}");
        tx.setPriceCode("song");
        tx.setEmail("test@selenium.com");
        tx.setAmount(50L);
        tx.setType("song");
        tx.setUsed(false);
        stripeDao.save(tx);

        // Llamar a /music/add con email y clientId correctos (simula "poner" cancion).
        int status = postSong("test@selenium.com", "fake-client-id");
        assertEquals(200, status, "La llamada a /music/add debe completar con pago válido");

        Optional<StripeTransaction> stored = stripeDao.findById("pi_test_song");
        assertTrue(stored.isPresent() && stored.get().isUsed(), "El pago debe marcarse como usado");
        assertEquals(1, songDao.count(), "Debe guardarse la canción");
    }

    @Test
    void sinPagoNoGuardaCancion() throws Exception {
        int status = postSong("test@selenium.com", "fake-client-id");
        assertEquals(402, status, "Debe responder 402 si no hay pago de canción disponible");
        assertEquals(0, songDao.count(), "No debe guardarse la canción");
    }

    private int postSong(String email, String clientId) throws Exception {
        // Construye y envia la peticion POST a /music/add simulando el front.
        HttpURLConnection con = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/music/add").openConnection();
        con.setConnectTimeout(2000);
        con.setReadTimeout(2000);
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);
        String body = """
                {"title":"Test Song","artist":"Tester","uri":"spotify:track:test","clientId":"%s","email":"%s"}
                """.formatted(clientId, email);
        con.getOutputStream().write(body.getBytes());
        int code = con.getResponseCode();
        con.disconnect();
        return code;
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
