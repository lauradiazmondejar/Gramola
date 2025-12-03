package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dao.SongDao;
import com.example.demo.dao.UserDao;
import com.example.demo.model.Song;
import com.example.demo.model.User;

@Service
public class MusicService {

    @Autowired
    private SongDao songDao;
    
    @Autowired
    private UserDao userDao;

    public void addSong(String title, String artist, String uri, String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new RuntimeException("ClientId vacío en la petición");
        }

        // Buscar al bar dueño de la sesión
        User bar = userDao.findFirstByClientId(clientId)
            .orElseThrow(() -> new RuntimeException("Bar no encontrado"));

        Song song = new Song();
        song.setTitle(title);
        song.setArtist(artist);
        song.setUri(uri);
        song.setBar(bar);

        songDao.save(song);
        System.out.println("Canción guardada: " + title + " para el bar: " + bar.getBar());
    }
}
