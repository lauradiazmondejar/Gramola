package com.example.demo.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Price;

@Repository
public interface PriceDao extends JpaRepository<Price, String> {
    // Repositorio para consultar los precios configurados
}
