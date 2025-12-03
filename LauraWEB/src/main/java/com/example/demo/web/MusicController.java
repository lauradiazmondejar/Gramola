package com.example.demo.web;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
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
    public void addSong(@RequestBody Map<String, String> body) {
        String title = body.get("title");
        String artist = body.get("artist");
        String uri = body.get("uri");
        String clientId = body.get("clientId"); // Necesitamos saber qué bar es

        this.musicService.addSong(title, artist, uri, clientId);
    }
}
