package com.example.demo;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Habilita el despliegue como WAR en un contenedor servlet tradicional.
 */
public class ServletInitializer extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		// Configura el despliegue tradicional apuntando a la clase principal.
		return application.sources(DemoApplication.class);
	}

}
