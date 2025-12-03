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
    @Id
    private String email;
    private String password;
    private String bar;
    private String clientId;
    private String clientSecret;
    private Double latitude;
    private Double longitude;
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "creation_token_id", referencedColumnName = "id")
    private Token creationToken;

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
}
