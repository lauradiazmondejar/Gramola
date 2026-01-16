package com.example.demo.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.model.Song;
import java.util.List;

/**
 * Repositorio de canciones en cola por bar.
 */
@Repository
public interface SongDao extends JpaRepository<Song, Long> {
    // Devuelve la cola de canciones de un bar ordenada por fecha.
    List<Song> findByBar_EmailOrderByDateAsc(String email);
}
