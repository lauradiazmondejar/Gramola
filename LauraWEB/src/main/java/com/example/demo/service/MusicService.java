package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dao.SongDao;
import com.example.demo.dao.UserDao;
import com.example.demo.model.Song;
import com.example.demo.model.User;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class MusicService {

    @Autowired
    private SongDao songDao;
    
    @Autowired
    private UserDao userDao;

    public void addSong(String title, String artist, String uri, String email, String clientId, Double userLat, Double userLon) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta el email del bar logueado");
        }
        if (clientId == null || clientId.isBlank()) {
            throw new RuntimeException("ClientId vacío en la petición");
        }

        // Buscar al bar dueño de la sesión por email
        User bar = userDao.findById(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bar no encontrado"));

        // Validar que el clientId enviado corresponde a ese bar
        if (bar.getClientId() == null || !bar.getClientId().equals(clientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El bar logueado no coincide con el clientId");
        }

        if (bar.getLatitude() != null && bar.getLongitude() != null) {
            
            if (userLat == null || userLon == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este bar requiere ubicación. Activa tu GPS.");
            }

            double distanciaMetros = calcularDistancia(bar.getLatitude(), bar.getLongitude(), userLat, userLon);
            
            System.out.println("Distancia calculada: " + distanciaMetros + " metros.");

            // Límite: 100 metros
            if (distanciaMetros > 100) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Estás demasiado lejos del bar (" + (int)distanciaMetros + "m). Acércate para pedir música.");
            }
        }
        
        Song song = new Song();
        song.setTitle(title);
        song.setArtist(artist);
        song.setUri(uri);
        song.setBar(bar);

        songDao.save(song);
        System.out.println("Canción guardada: " + title + " para el bar: " + bar.getBar());
    }

    private double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radio de la Tierra en km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        double distanciaKm = R * c; 
        return distanciaKm * 1000; // Convertimos a metros
    }
}
