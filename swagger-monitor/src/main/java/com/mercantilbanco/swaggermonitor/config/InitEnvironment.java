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

            // ✅ path dinámico del XML
            String pathFile = Utils.getFileAppConfig(
                    BusinessCard.APP.getValue()
            );

            try (InputStream is = new FileInputStream(pathFile)) {

                props.loadFromXML(is);

                // =====================================================
                // ✅ PATHS
                // =====================================================
                String pathLog = Utils.resolvePath(
                        props.getProperty(BusinessCard.LOG4J.getValue())
                );

                String pathSwagger = Utils.resolvePath(
                        props.getProperty(BusinessCard.SWAGGER_OUTPUT.getValue())
                );

                String pathServers = Utils.resolvePath(
                        props.getProperty(BusinessCard.SERVERS_FILE.getValue())
                );

                // =====================================================
                // ✅ CONFIGURACIONES
                // =====================================================
                String refresh = props.getProperty(
                        BusinessCard.REFRESH.getValue()
                );

                String slack = props.getProperty(
                        BusinessCard.SLACK_ENABLED.getValue()
                );

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
                // ✅ VALIDACIÓN DE EXISTENCIA
                // =====================================================
                FileUtility.validatePaths(
                        pathSwagger,
                        pathServers
                );

                // =====================================================
                // ✅ ENTORNO DE TEST
                // =====================================================
                String testEnv = props.getProperty(
                        BusinessCard.TEST_ENVIRONMENT.getValue()
                );

                if (!"true".equalsIgnoreCase(testEnv)) {

                    // ✅ validar log solo si aplica
                    if (pathLog != null && !pathLog.trim().isEmpty()) {
                        FileUtility.validatePaths(pathLog);
                        Utils.setConfigLog4j(pathLog);
                    }

                } else {
                    System.out.println("⚠️ Log4j no inicializado en entorno de test");
                }

                // =====================================================
                // ✅ SYSTEM PROPERTIES (OSBA)
                // =====================================================
                System.setProperty(
                        BusinessCard.SWAGGER_OUTPUT.getValue(),
                        pathSwagger
                );

                System.setProperty(
                        BusinessCard.SERVERS_FILE.getValue(),
                        pathServers
                );

                if (refresh != null) {
                    System.setProperty(
                            BusinessCard.REFRESH.getValue(),
                            refresh
                    );
                }

                if (slack != null) {
                    System.setProperty(
                            BusinessCard.SLACK_ENABLED.getValue(),
                            slack
                    );
                }

                System.out.println("✅ Swagger Monitor configurado");

            }

        } catch (ExceptionFile e) {

            throw new RuntimeException(
                    "❌ Error en paths configurados: " + e.getMessage(), e
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "❌ Error cargando configuración monitor", e
            );
        }
    }
}