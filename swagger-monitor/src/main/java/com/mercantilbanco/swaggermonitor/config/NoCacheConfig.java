package com.mercantilbanco.swaggermonitor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CLASE ENCARGADA DE: <strong>NO guardar cache, Siempre pide los archivos al servidor, Siempre ves cambios en tiempo real</strong>.
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

