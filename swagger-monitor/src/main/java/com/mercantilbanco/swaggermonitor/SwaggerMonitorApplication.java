package com.mercantilbanco.swaggermonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.mercantilbanco.swaggermonitor.config.InitEnvironment;

@SpringBootApplication
public class SwaggerMonitorApplication {

    public static void main(String[] args) {

        // ✅ OSBA INIT
        InitEnvironment.getConfig();

        SpringApplication.run(SwaggerMonitorApplication.class, args);
    }
}