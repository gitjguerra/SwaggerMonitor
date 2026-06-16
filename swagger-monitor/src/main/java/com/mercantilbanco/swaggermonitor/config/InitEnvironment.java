package com.mercantilbanco.swaggermonitor.config;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mercantilbanco.swaggermonitor.util.Utils;
import com.mercantilbanco.swaggermonitor.util.FileUtility;
import com.mercantilbanco.swaggermonitor.exception.ExceptionFile;

public class InitEnvironment {

    private static final Logger log =
            LogManager.getLogger(InitEnvironment.class);

    private InitEnvironment() {}

    public static void getConfig() {

        Properties props = new Properties();

        try {

            String pathFile = Utils.getFileAppConfig(
                    BusinessCard.APP.getValue()
            );

            try (InputStream is = new FileInputStream(pathFile)) {

                props.loadFromXML(is);

                // =====================================================
                // ✅ CONFIGURAR ENTORNO (IMPORTANTE PARA LOG4J)
                // =====================================================
                String testEnv = props.getProperty(
                        BusinessCard.TEST_ENVIRONMENT.getValue()
                );

                Utils.setListtags(
                        java.nio.file.Paths.get(pathFile).getParent(),
                        Boolean.parseBoolean(testEnv)
                );

                // =====================================================
                // ✅ RESOLVER PATHS
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

                String refresh = props.getProperty(
                        BusinessCard.REFRESH.getValue()
                );

                String slackEnabled = props.getProperty(
                        BusinessCard.SLACK_ENABLED.getValue()
                );

                String slackWebhook = props.getProperty(
                        BusinessCard.SLACK_WEBHOOK.getValue()
                );

                // =====================================================
                // ✅ SYSTEM PROPERTIES (ANTES DE LOG4J)
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

                if (slackEnabled != null) {
                    System.setProperty(
                            BusinessCard.SLACK_ENABLED.getValue(),
                            slackEnabled
                    );
                }

                if (slackWebhook != null && !slackWebhook.trim().isEmpty()) {
                    System.setProperty(
                            BusinessCard.SLACK_WEBHOOK.getValue(),
                            slackWebhook
                    );
                }

                // =====================================================
                // ✅ VALIDACIONES
                // =====================================================
                if (pathSwagger == null || pathSwagger.trim().isEmpty()) {
                    throw new RuntimeException("❌ pathSwaggerOutput no configurado");
                }

                if (pathServers == null || pathServers.trim().isEmpty()) {
                    throw new RuntimeException("❌ pathServersFile no configurado");
                }

                FileUtility.validatePaths(
                        pathSwagger,
                        pathServers
                );

                // =====================================================
                // ✅ CREAR CARPETA LOGS (🔥 CLAVE)
                // =====================================================
                String bmHome = System.getenv("BM_HOME");

                if (bmHome != null) {
                    new java.io.File(
                            bmHome + "/appl/swagger-monitor/logs"
                    ).mkdirs();
                }

                // =====================================================
                // ✅ CONFIGURAR LOG4J (AHORA SÍ)
                // =====================================================
                if (!"true".equalsIgnoreCase(testEnv)) {

                    if (pathLog != null && !pathLog.trim().isEmpty()) {
                        FileUtility.validatePaths(pathLog);
                        Utils.setConfigLog4j(pathLog);
                    }

                } else {
                    System.out.println(
                            "⚠️ Log4j no inicializado en entorno de test"
                    );
                }

                // =====================================================
                // ✅ LOG FINAL (YA FUNCIONA)
                // =====================================================
                log.info("✅ APLICACION [{}] CONFIGURADA EXITOSAMENTE",
                        BusinessCard.APP.getValue());

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