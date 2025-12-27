package com.example.demo.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;


@Entity
@Table(name = "bars")
public class User {
    // Email del bar usado como identificador
    @Id
    private String email;
    private String password;
    // Nombre comercial del bar
    private String bar;
    private String clientId;
    private String clientSecret;
    private Double latitude;
    private Double longitude;
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "creation_token_id", referencedColumnName = "id")
    private Token creationToken;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "reset_token_id", referencedColumnName = "id")
    private Token resetToken;

    @jakarta.persistence.Lob
    @jakarta.persistence.Column(columnDefinition = "LONGTEXT")
    private String signature;

    private boolean paid = false;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getBar() { return bar; }
    public void setBar(String bar) { this.bar = bar; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public Token getCreationToken() { return creationToken; }
    public void setCreationToken(Token token) {
        this.creationToken = token;
    }

    public Token getResetToken() {
        return resetToken;
    }

    public void setResetToken(Token resetToken) {
        this.resetToken = resetToken;
    }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }
}
