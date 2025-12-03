package com.example.demo.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.model.Song;

@Repository
public interface SongDao extends JpaRepository<Song, Long> {
    // Aquí podríamos añadir métodos como findByBar, etc.
}