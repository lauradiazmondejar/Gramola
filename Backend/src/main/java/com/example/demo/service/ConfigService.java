package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dao.ConfigDao;

/**
 * Proporciona acceso a la configuracion global almacenada en BD.
 * Sustituye los valores hardcodeados o en application.properties por entradas en la tabla config.
 */
@Service
public class ConfigService {

    @Autowired
    private ConfigDao configDao;

    public String getValue(String key) {
        return configDao.findById(key)
                .map(c -> c.getValue())
                .orElseThrow(() -> new IllegalStateException("Config no encontrada para la clave: " + key));
    }

    public String getValue(String key, String defaultValue) {
        return configDao.findById(key)
                .map(c -> c.getValue())
                .orElse(defaultValue);
    }
}
