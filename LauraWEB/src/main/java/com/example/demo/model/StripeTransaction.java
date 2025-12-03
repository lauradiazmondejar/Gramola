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
    
    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
}