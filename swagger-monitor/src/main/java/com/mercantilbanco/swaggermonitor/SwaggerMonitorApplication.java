package com.mercantilbanco.swaggermonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import com.mercantilbanco.swaggermonitor.config.InitEnvironment;

@SpringBootApplication
public class SwaggerMonitorApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {

        // ✅ OSBA INIT
        InitEnvironment.getConfig();

        return builder.sources(SwaggerMonitorApplication.class);
    }

    public static void main(String[] args) {

        // ✅ OSBA INIT
        InitEnvironment.getConfig();

        SpringApplication.run(SwaggerMonitorApplication.class, args);
    }
}