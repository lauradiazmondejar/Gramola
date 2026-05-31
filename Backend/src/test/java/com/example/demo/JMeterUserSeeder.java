package com.example.demo;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.dao.ConfigDao;
import com.example.demo.dao.UserDao;
import com.example.demo.model.Config;
import com.example.demo.model.User;

/**
 * Siembra 1000 usuarios en BD para el test de carga con JMeter.
 * Cada usuario tiene email userNNN@test.com y contraseña "12345678".
 *
 * Ejecutar ANTES del plan JMeter con:
 *   mvn test -DrunJMeterSeeder=true
 *
 * Para limpiar los usuarios tras el test:
 *   mvn test -DrunJMeterSeeder=true -DjmeterClean=true
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = { "server.port=8080" })
class JMeterUserSeeder {

    @Autowired
    private UserDao userDao;

    @Autowired
    private ConfigDao configDao;

    private static final String PASSWORD_PLAIN = "12345678";
    private static final int NUM_USERS = 1000;

    @Test
    void seedUsuariosParaJMeter() {
        assumeTrue(
            Boolean.parseBoolean(System.getProperty("runJMeterSeeder", "false")),
            "Seeder desactivado — añade -DrunJMeterSeeder=true para ejecutarlo"
        );

        // URLs en config necesarias para que el contexto arranque correctamente.
        seedConfig("spotify.api.url",   "https://api.spotify.com/v1");
        seedConfig("spotify.token.url", "https://accounts.spotify.com/api/token");
        seedConfig("app.frontend.url",  "http://127.0.0.1:4200");
        seedConfig("app.backend.url",   "http://localhost:8080");
        seedConfig("app.callback.url",  "http://127.0.0.1:4200/callback");

        boolean clean = Boolean.parseBoolean(System.getProperty("jmeterClean", "false"));
        if (clean) {
            // Elimina solo los usuarios de carga para no afectar datos reales.
            for (int i = 1; i <= NUM_USERS; i++) {
                String email = String.format("user%03d@test.com", i);
                userDao.deleteById(email);
            }
            System.out.printf("Limpiados %d usuarios de carga JMeter.%n", NUM_USERS);
            return;
        }

        String hashedPassword = DigestUtils.sha512Hex(PASSWORD_PLAIN);
        int creados = 0;

        for (int i = 1; i <= NUM_USERS; i++) {
            String email = String.format("user%03d@test.com", i);

            // No sobreescribir si ya existe.
            if (userDao.existsById(email)) continue;

            User user = new User();
            user.setEmail(email);
            user.setBar(String.format("Bar %03d", i));
            user.setPassword(hashedPassword);
            user.setClientId(String.format("client-%03d", i));
            user.setClientSecret("fake-secret-jmeter");
            user.setPaid(true); // Requerido para que el login devuelva 200.
            userDao.save(user);
            creados++;
        }

        System.out.printf("Seeder JMeter completado: %d usuarios creados (email: user001@test.com .. user%d@test.com, password: %s).%n",
            creados, NUM_USERS, PASSWORD_PLAIN);
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
