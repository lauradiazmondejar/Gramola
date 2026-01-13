package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class StripeTransaction {
    @Id
    private String id; // El ID que nos da Stripe (ej: pi_3M...)
    
    @Column(length = 10000) // Usamos un campo largo para guardar todo el JSON de respuesta
    private String data; 

    private String priceCode;

    /**
     * Amount in cents as sent to Stripe. Persistimos para trazabilidad y para
     * verificar que usamos el importe correcto desde BD.
     */
    private Long amount;

    private String email;
    private String bar;
    // Diferencia pagos de suscripcion y de cancion
    private String type; // e.g. subscription, song
    // Marca si ya se uso el pago (para evitar reutilizaciones)
    private boolean used = false;

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public String getPriceCode() {
        return priceCode;
    }

    public void setPriceCode(String priceCode) {
        this.priceCode = priceCode;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBar() {
        return bar;
    }

    public void setBar(String bar) {
        this.bar = bar;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
