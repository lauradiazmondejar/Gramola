package com.example.demo.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import com.example.demo.model.User;

/**
 * Acceso a datos de usuarios (bares) usando Spring Data JPA.
 */
@Repository
public interface UserDao extends JpaRepository<User, String> {
    // Busquedas auxiliares por tokens y clientId.
    Optional<User> findByCreationToken_Id(String tokenId);

    Optional<User> findFirstByClientId(String clientId);

    Optional<User> findByResetToken_Id(String tokenId);
}
