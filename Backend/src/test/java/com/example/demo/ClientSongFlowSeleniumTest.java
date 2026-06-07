package com.example.demo;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.List;

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

import com.example.demo.dao.ConfigDao;
import com.example.demo.dao.PriceDao;
import com.example.demo.dao.SongDao;
import com.example.demo.dao.StripeTransactionDao;
import com.example.demo.dao.UserDao;
import com.example.demo.model.Config;
import com.example.demo.model.Price;
import com.example.demo.model.Song;
import com.example.demo.model.StripeTransaction;
import com.example.demo.model.User;

import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * E2E Selenium: un cliente del bar busca una cancion, paga y la pone.
 * Verifica que el pago queda confirmado en BD y la cancion aparece
 * en la lista de canciones del bar en el backend.
 *
 * Requiere: front en 127.0.0.1:4200 y backend en 8080.
 * Ejecutar con: mvn test -DrunClientSongTests=true
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = { "server.port=8080" })
class ClientSongFlowSeleniumTest {

    @Value("${e2e.base-url:http://127.0.0.1:4200}")
    private String baseUrl;

    @Autowired private UserDao userDao;
    @Autowired private SongDao songDao;
    @Autowired private StripeTransactionDao stripeDao;
    @Autowired private PriceDao priceDao;
    @Autowired private ConfigDao configDao;

    private WebDriver driver;
    private WebDriverWait wait;

    private static final String BAR_EMAIL    = "bar-selenium@test.com";
    private static final String BAR_CLIENTID = "selenium-client-id";
    private static final String STRIPE_TX_ID = "pi_test_client_song_flow";

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setup() {
        assumeTrue(
            Boolean.parseBoolean(System.getProperty("runClientSongTests", "false")),
            "Test desactivado — añade -DrunClientSongTests=true para ejecutarlo"
        );
        assumeTrue(frontendUp(), "Front no disponible en " + baseUrl);

        // Limpieza previa (orden: songs → stripe → users para respetar FK).
        songDao.deleteAll();
        stripeDao.deleteAll();
        userDao.deleteAll();

        // Bar de prueba con suscripción pagada (simula kiosco ya configurado).
        User bar = new User();
        bar.setEmail(BAR_EMAIL);
        bar.setBar("Bar Selenium Test");
        bar.setPassword("hashedpwd");
        bar.setClientId(BAR_CLIENTID);
        bar.setClientSecret("fake-secret");
        bar.setPaid(true);
        userDao.save(bar);

        // Precio de canción en BD (requisito del enunciado: precios en BD).
        seedPrice("song", "Cancion individual", 50L);

        // URLs en tabla config (requerido por ConfigService en el contexto de prueba).
        seedConfig("spotify.api.url",   "https://api.spotify.com/v1");
        seedConfig("spotify.token.url", "https://accounts.spotify.com/api/token");
        seedConfig("app.frontend.url",  "http://127.0.0.1:4200");
        seedConfig("app.backend.url",   "http://localhost:8080");
        seedConfig("app.callback.url",  "http://127.0.0.1:4200/callback");

        // Pago de canción pre-creado: simula que el cliente ya confirmó en Stripe.
        StripeTransaction tx = new StripeTransaction();
        tx.setId(STRIPE_TX_ID);
        tx.setData("{}");
        tx.setPriceCode("song");
        tx.setEmail(BAR_EMAIL);
        tx.setAmount(50L);
        tx.setType("song");
        tx.setUsed(false);
        stripeDao.save(tx);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu", "--remote-allow-origins=*");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @AfterEach
    void teardown() {
        if (driver != null) driver.quit();
        songDao.deleteAll();
        stripeDao.deleteAll();
        userDao.deleteAll();
    }

    @Test
    void clienteBuscaPagaYPoneCancion() throws Exception {

        // 1. Abrir la app e inyectar la sesion del bar en sessionStorage.
        //    Representa el kiosco del bar donde el propietario ya ha iniciado sesion.
        driver.get(baseUrl + "/music");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(
            "sessionStorage.setItem('email', arguments[0]);" +
            "sessionStorage.setItem('clientId', arguments[1]);" +
            "sessionStorage.setItem('bar', 'Bar Selenium Test');",
            BAR_EMAIL, BAR_CLIENTID
        );

        // 2. Recargar para que Angular lea la sesion y muestre el panel de musica.
        driver.navigate().refresh();
        wait.until(ExpectedConditions.urlContains("/music"));

        // 3. Verificar que la pagina de busqueda de canciones esta visible.
        List<WebElement> searchInputs = driver.findElements(
            By.cssSelector("input[placeholder*='rtista'], input[placeholder*='ancion']")
        );
        assertTrue(searchInputs.size() > 0,
            "El campo de busqueda debe estar visible en la pagina de musica");

        // 4. El cliente escribe el titulo de la cancion que quiere poner.
        searchInputs.get(0).sendKeys("Bohemian Rhapsody");

        // 5. Simular el pago y la insercion de la cancion via la API del backend.
        //    Equivale al flujo: cliente confirma Stripe → front llama a /music/add.
        int status = postSong(BAR_EMAIL, BAR_CLIENTID,
            "Bohemian Rhapsody", "Queen", "spotify:track:4u7EnebtmKWzUH433cf5Qv");
        assertEquals(200, status,
            "POST /music/add debe responder 200 cuando el pago es valido");

        // 6. Verificar en BD que el pago queda CONFIRMADO (marcado como usado).
        StripeTransaction savedTx = stripeDao.findById(STRIPE_TX_ID).orElseThrow(
            () -> new AssertionError("La transaccion de Stripe no existe en BD")
        );
        assertTrue(savedTx.isUsed(),
            "El pago debe quedar marcado como usado en BD tras insertar la cancion");

        // 7. Verificar en BD que la cancion se ha AÑADIDO a la lista del bar.
        List<Song> songs = songDao.findByBar_EmailOrderByPriorityDescDateAsc(BAR_EMAIL);
        assertEquals(1, songs.size(),
            "Debe haber exactamente 1 cancion en la lista del bar");

        Song saved = songs.get(0);
        assertEquals("Bohemian Rhapsody", saved.getTitle(),
            "El titulo de la cancion guardada debe coincidir");
        assertEquals("Queen", saved.getArtist(),
            "El artista de la cancion guardada debe coincidir");
        assertEquals(BAR_EMAIL, saved.getBar().getEmail(),
            "La cancion debe pertenecer al bar correcto");
        assertTrue(saved.isPriority(),
            "La cancion pagada debe tener priority=true (se cuela en la cola)");
    }

    // --- Helpers ---

    private int postSong(String email, String clientId,
                         String title, String artist, String uri) throws Exception {
        HttpURLConnection con = (HttpURLConnection)
            new URL("http://127.0.0.1:8080/music/add").openConnection();
        con.setConnectTimeout(3000);
        con.setReadTimeout(3000);
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);
        String body = """
                {"title":"%s","artist":"%s","uri":"%s","clientId":"%s","email":"%s"}
                """.formatted(title, artist, uri, clientId, email);
        con.getOutputStream().write(body.getBytes());
        int code = con.getResponseCode();
        con.disconnect();
        return code;
    }

    private boolean frontendUp() {
        try {
            HttpURLConnection con = (HttpURLConnection)
                new URL(baseUrl + "/").openConnection();
            con.setConnectTimeout(2000);
            con.setReadTimeout(2000);
            con.setRequestMethod("GET");
            return con.getResponseCode() < 500;
        } catch (Exception e) {
            return false;
        }
    }

    private void seedPrice(String code, String desc, long amount) {
        priceDao.findById(code).ifPresentOrElse(p -> {}, () -> {
            Price p = new Price();
            p.setCode(code);
            p.setDescription(desc);
            p.setAmount(amount);
            priceDao.save(p);
        });
    }

    private void seedConfig(String key, String value) {
        configDao.findById(key).ifPresentOrElse(c -> {}, () -> {
            Config c = new Config();
            c.setId(key);
            c.setValue(value);
            configDao.save(c);
        });
    }
}
