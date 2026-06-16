package com.mercantilbanco.swaggermonitor.config;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import com.mercantilbanco.swaggermonitor.util.Utils;

public class InitEnvironment {

    private InitEnvironment() {}

    public static void getConfig() {

        try {

            Properties props = new Properties();

            // ✅ obtiene path BM_HOME/appl/swagger-monitor/config/swagger-monitor.xml
            String pathFile = Utils.getFileAppConfig("swagger-monitor");

            try (InputStream is = new FileInputStream(pathFile)) {

                props.loadFromXML(is);

                String pathLog = Utils.resolvePath(
                        props.getProperty("pathLog4jFile")
                );

                String pathSwagger = Utils.resolvePath(
                        props.getProperty("pathSwaggerOutput")
                );

                String pathServers = Utils.resolvePath(
                        props.getProperty("pathServersFile")
                );

                // =====================================================
                // ✅ DETECTAR ENTORNO DE TEST
                // =====================================================
                String testEnv = props.getProperty("testEnvironment");

                // ✅ logging SOLO si NO es test
                if (!"true".equalsIgnoreCase(testEnv)) {
                    Utils.setConfigLog4j(pathLog);
                } else {
                    System.out.println("⚠️ Log4j no inicializado en entorno de test");
                }

                // =====================================================
                // ✅ set properties globales (ESTILO OSBA)
                // =====================================================
                System.setProperty("pathSwaggerOutput", pathSwagger);
                System.setProperty("pathServersFile", pathServers);

                System.out.println("✅ Swagger Monitor configurado");

            }

        } catch (Exception e) {
            throw new RuntimeException("❌ Error cargando configuración monitor", e);
        }
    }
}