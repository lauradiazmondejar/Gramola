package com.example.demo.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import com.example.demo.model.User;

@Repository
public interface UserDao extends JpaRepository<User, String> {
    Optional<User> findByCreationToken_Id(String tokenId);

    Optional<User> findFirstByClientId(String clientId);
}
