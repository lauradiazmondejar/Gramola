package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del backend Spring Boot.
 * Orquesta el arranque de controladores, servicios y repositorios (MVC por capas).
 */
@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
		// Arranca la aplicacion Spring Boot cargando todos los beans y configuraciones.
		SpringApplication.run(DemoApplication.class, args);
	}

}
