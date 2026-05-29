package com.mercantilbanco.monitor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CLASE ENCARGADA DE: <strong>XXX</strong>.
 * @author <strong>JOSE GUERRA</strong>
 * <hr><code>Copyright 29/05/2026. Banco Mercantil, All rights reserved.</code>
 */

@Configuration
public class NoCacheConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler("/swagger/**")
                .addResourceLocations("classpath:/static/swagger/")
                .setCachePeriod(0); // 🔥 DESACTIVA CACHE COMPLETAMENTE
    }
}

