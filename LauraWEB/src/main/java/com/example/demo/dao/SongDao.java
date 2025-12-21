package com.example.demo.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.model.Song;
import java.util.List;

@Repository
public interface SongDao extends JpaRepository<Song, Long> {
    List<Song> findByBar_EmailOrderByDateAsc(String email);
}
