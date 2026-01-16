package com.example.demo.model;

import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Id;
import jakarta.persistence.Column;

/**
 * Token de confirmacion de cuenta o de reseteo de password.
 */
@Entity
public class Token {
    // Identificador unico usado en confirmaciones y reseteos.
    @Id @Column(length = 36)
    private String id;
    // Instante de creacion del token.
    private long creationtime;
    // Instante en que se marco como usado (0 si sigue activo).
    private long usetime= 0;
    @OneToOne(mappedBy = "creationToken")
    private User user;

    public Token(){
        this.id = UUID.randomUUID().toString();
        this.creationtime = System.currentTimeMillis();
    }



    public void use(){
        this.usetime = System.currentTimeMillis();
    }

    public boolean isUsed(){
        return this.usetime != 0;
    }

    public String getId() {
        return id;
    }

    public long getCreationTime() {
        return creationtime;
    }

    public long getUseTime() {
        return usetime;
    }
}
