package com.example.demo.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.model.Token;

/**
 * Repositorio para tokens de confirmacion y reseteo.
 */
@Repository
public interface TokenDao extends JpaRepository<Token, String> {
    // Marcador: la interfaz hereda operaciones CRUD basicas.
}
