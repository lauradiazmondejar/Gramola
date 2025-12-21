package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dao.SongDao;
import com.example.demo.dao.StripeTransactionDao;
import com.example.demo.dao.UserDao;
import com.example.demo.model.StripeTransaction;
import com.example.demo.model.User;

@SpringBootTest
class MusicServiceTests {

    @Autowired
    private MusicService musicService;

    @Autowired
    private UserDao userDao;

    @Autowired
    private StripeTransactionDao stripeDao;

    @Autowired
    private SongDao songDao;

    @BeforeEach
    void clean() {
        songDao.deleteAll();
        stripeDao.deleteAll();
        userDao.deleteAll();
    }

    @Test
    void addSongConsumesPaymentAndSaves() {
        User bar = baseUser(false);
        userDao.save(bar);

        StripeTransaction tx = new StripeTransaction();
        tx.setId("pi_song_ok");
        tx.setData("{}");
        tx.setPriceCode("song");
        tx.setEmail(bar.getEmail());
        tx.setAmount(50L);
        tx.setType("song");
        stripeDao.save(tx);

        musicService.addSong("Test", "Tester", "spotify:track:test", bar.getEmail(), bar.getClientId(), null, null);

        assertEquals(1, songDao.count(), "Debe guardarse la canción");
        assertTrue(stripeDao.findById("pi_song_ok").get().isUsed(), "El pago debe marcarse como usado");
    }

    @Test
    void addSongWithoutPaymentReturns402() {
        User bar = baseUser(false);
        userDao.save(bar);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                musicService.addSong("Test", "Tester", "spotify:track:test", bar.getEmail(), bar.getClientId(), null, null));
        assertEquals(402, ex.getStatusCode().value());
        assertEquals(0, songDao.count(), "No debe guardarse la canción");
    }

    @Test
    void addSongOutsideGeofenceReturns403() {
        User bar = baseUser(true);
        userDao.save(bar);

        StripeTransaction tx = new StripeTransaction();
        tx.setId("pi_song_geo");
        tx.setData("{}");
        tx.setPriceCode("song");
        tx.setEmail(bar.getEmail());
        tx.setAmount(50L);
        tx.setType("song");
        stripeDao.save(tx);

        // Coordenadas lejanas (>100m)
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                musicService.addSong("Test", "Tester", "spotify:track:test", bar.getEmail(), bar.getClientId(),
                        bar.getLatitude() + 0.01, bar.getLongitude() + 0.01));
        assertEquals(403, ex.getStatusCode().value());
        assertEquals(0, songDao.count(), "No debe guardarse la canción si está fuera de rango");
    }

    private User baseUser(boolean withLocation) {
        User user = new User();
        user.setEmail("bar@test.com");
        user.setBar("Bar Test");
        user.setPassword("12345678");
        user.setClientId("client-id");
        user.setClientSecret("secret");
        if (withLocation) {
            user.setLatitude(39.0);
            user.setLongitude(-3.0);
        }
        return user;
    }
}
