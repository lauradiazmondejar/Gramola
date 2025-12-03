package com.example.demo.model;

import java.util.Date;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;

@Entity
public class Song {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String artist;
    private String uri; // El identificador de Spotify (spotify:track:...)
    
    private Date date;

    // Relación con el Bar (User)
    @ManyToOne
    private User bar;

    @PrePersist
    protected void onCreate() {
        date = new Date(); // Se guarda la fecha actual automáticamente
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    
    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }
    
    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }
    
    public User getBar() { return bar; }
    public void setBar(User bar) { this.bar = bar; }
}
