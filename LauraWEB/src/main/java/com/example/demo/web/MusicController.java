package com.example.demo.web;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.MusicService;

@RestController
@RequestMapping("music")
@CrossOrigin(origins = {"http://127.0.0.1:4200"}) // Front se sirve en 127.0.0.1
public class MusicController {

    @Autowired
    private MusicService musicService;

    @PostMapping("/add")
    public void addSong(@RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String artist = (String) body.get("artist");
        String uri = (String) body.get("uri");
        String email = (String) body.get("email");
        String clientId = (String) body.get("clientId");

        Double userLat = body.get("lat") != null ? Double.valueOf(body.get("lat").toString()) : null;
        Double userLon = body.get("lon") != null ? Double.valueOf(body.get("lon").toString()) : null;

        this.musicService.addSong(title, artist, uri, email, clientId, userLat, userLon);
    }

    @GetMapping("/health")
    public String health() {
        return "ok";
    }
}
