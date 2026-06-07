package com.example.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Par clave-valor para configuracion global de la aplicacion.
 * Permite cambiar URLs y otros parametros sin tocar el codigo.
 */
@Entity
public class Config {

    @Id
    private String id; // clave descriptiva, p.ej. "spotify.api.url"

    private String value;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
