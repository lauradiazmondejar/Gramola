package com.example.demo.model;

import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Id;
import jakarta.persistence.Column;

@Entity
public class Token {
    @Id @Column(length = 36)
    private String id;
    private long creationtime;
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
