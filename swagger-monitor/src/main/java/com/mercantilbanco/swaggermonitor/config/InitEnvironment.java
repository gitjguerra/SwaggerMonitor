package com.mercantilbanco.swaggermonitor.config;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import com.mercantilbanco.swaggermonitor.util.Utils;
import com.mercantilbanco.swaggermonitor.util.FileUtility;
import com.mercantilbanco.swaggermonitor.exception.ExceptionFile;

public class InitEnvironment {

    private InitEnvironment() {}

    public static void getConfig() {

        try {

            Properties props = new Properties();

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

                String refresh = props.getProperty("monitorRefresh");
                String slack = props.getProperty("slackEnabled");

                // =====================================================
                // ✅ VALIDACIONES BÁSICAS
                // =====================================================
                if (pathSwagger == null || pathSwagger.trim().isEmpty()) {
                    throw new RuntimeException("❌ pathSwaggerOutput no configurado");
                }

                if (pathServers == null || pathServers.trim().isEmpty()) {
                    throw new RuntimeException("❌ pathServersFile no configurado");
                }

                // =====================================================
                // ✅ VALIDACIÓN DE EXISTENCIA (🔥 NUEVO)
                // =====================================================
                FileUtility.validatePaths(
                        pathSwagger,
                        pathServers
                );

                // =====================================================
                // ✅ ENTORNO DE TEST
                // =====================================================
                String testEnv = props.getProperty("testEnvironment");

                if (!"true".equalsIgnoreCase(testEnv)) {
                    FileUtility.validatePaths(pathLog); // ✅ opcional pero recomendado
                    Utils.setConfigLog4j(pathLog);
                } else {
                    System.out.println("⚠️ Log4j no inicializado en entorno de test");
                }

                // =====================================================
                // ✅ SYSTEM PROPERTIES (OSBA)
                // =====================================================
                System.setProperty("pathSwaggerOutput", pathSwagger);
                System.setProperty("pathServersFile", pathServers);

                if (refresh != null) {
                    System.setProperty("monitorRefresh", refresh);
                }

                if (slack != null) {
                    System.setProperty("slackEnabled", slack);
                }

                System.out.println("✅ Swagger Monitor configurado");

            }

        } catch (ExceptionFile e) {

            throw new RuntimeException("❌ Error en paths configurados: " + e.getMessage(), e);

        } catch (Exception e) {

            throw new RuntimeException("❌ Error cargando configuración monitor", e);
        }
    }
}