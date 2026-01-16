package com.example.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Entidad de precio configurable en BD.
 */
@Entity
public class Price {
    // Codigo usado para identificar el tipo de producto.
    @Id
    private String code; // e.g. subscription_monthly, subscription_annual, song

    private String description;

    /**
     * Amount in cents of euro.
     */
    private long amount;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }
}
