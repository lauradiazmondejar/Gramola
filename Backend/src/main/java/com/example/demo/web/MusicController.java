package com.example.demo.web;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.MusicService;

/**
 * Endpoints de la gramola: alta de canciones y consulta de cola.
 * Capa web que delega en MusicService (reglas de pago y geolocalizacion).
 */
@RestController
@RequestMapping("music")
@CrossOrigin(origins = {"http://127.0.0.1:4200"}) // Front se sirve en 127.0.0.1
public class MusicController {

    @Autowired
    private MusicService musicService;

    @PostMapping("/add")
    public void addSong(@RequestBody Map<String, Object> body) {
        // Mapea el cuerpo JSON a campos individuales para validarlos.
        String title = (String) body.get("title");
        String artist = (String) body.get("artist");
        String uri = (String) body.get("uri");
        String email = (String) body.get("email");
        String clientId = (String) body.get("clientId");

        Double userLat = body.get("lat") != null ? Double.valueOf(body.get("lat").toString()) : null;
        Double userLon = body.get("lon") != null ? Double.valueOf(body.get("lon").toString()) : null;

        // Pasa la peticion al servicio, que valida y persiste la cancion.
        this.musicService.addSong(title, artist, uri, email, clientId, userLat, userLon);
    }

    @PostMapping("/add-free")
    public void addFree(@RequestBody Map<String, Object> body) {
        // El propietario añade canciones al catalogo del bar sin coste
        // La contraseña es obligatoria para evitar que los clientes añadan gratis desde el kiosco
        String title = (String) body.get("title");
        String artist = (String) body.get("artist");
        String uri = (String) body.get("uri");
        String email = (String) body.get("email");
        String clientId = (String) body.get("clientId");
        String password = (String) body.get("password");
        this.musicService.addSongFree(title, artist, uri, email, clientId, password);
    }

    @GetMapping("/health")
    public String health() {
        return "ok";
    }

    @GetMapping("/queue")
    public java.util.List<com.example.demo.model.Song> listQueue(@RequestParam String email) {
        // Devuelve la cola de canciones asociada al bar del usuario.
        return musicService.listSongsForBar(email);
    }
}
