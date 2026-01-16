package com.example.demo.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Price;

/**
 * Repositorio de precios configurados en BD (suscripciones y cancion).
 */
@Repository
public interface PriceDao extends JpaRepository<Price, String> {
    // Marcador: la interfaz hereda operaciones CRUD basicas.
}
