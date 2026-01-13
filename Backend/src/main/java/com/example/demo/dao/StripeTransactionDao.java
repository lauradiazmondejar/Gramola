package com.example.demo.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.StripeTransaction;

@Repository
public interface StripeTransactionDao extends JpaRepository<StripeTransaction, String> {

    // Recupera el ultimo pago no consumido por email y tipo de precio
    Optional<StripeTransaction> findFirstByEmailAndPriceCodeAndUsedFalse(String email, String priceCode);
}
