package com.mercantilbanco.swaggermonitor.config;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mercantilbanco.swaggermonitor.util.Utils;
import com.mercantilbanco.swaggermonitor.util.FileUtility;
import com.mercantilbanco.swaggermonitor.exception.ExceptionFile;

public class InitEnvironment {

    private static final Logger log = LogManager.getLogger(InitEnvironment.class);

    private InitEnvironment() {}

    public static void getConfig() {

        Properties props = new Properties();

        String pathFile;
        String pathLog;
        String pathSwagger;
        String pathServers;

        try {

            // =================================================
            // ✅ CONFIG FILE
            // =================================================
            pathFile = Utils.getFileAppConfig(BusinessCard.APP.getValue());

            try (InputStream is = new FileInputStream(pathFile)) {

                props.loadFromXML(is);

                // =================================================
                // ✅ ENTORNO
                // =================================================
                Utils.setListtags(Paths.get(pathFile).getParent(), Boolean.parseBoolean(props.getProperty(BusinessCard.TEST_ENVIRONMENT.getValue())));

                // =================================================
                // ✅ PATHS
                // =================================================
                pathLog = Utils.resolvePath(props.getProperty(BusinessCard.LOG4J.getValue()));

                pathSwagger = Utils.resolvePath(props.getProperty(BusinessCard.SWAGGER_OUTPUT.getValue()));

                pathServers = Utils.resolvePath(props.getProperty(BusinessCard.SERVERS_FILE.getValue()));

                // =================================================
                // ✅ SYSTEM PROPERTIES
                // =================================================
                System.setProperty(BusinessCard.SWAGGER_OUTPUT.getValue(), pathSwagger);

                System.setProperty(BusinessCard.SERVERS_FILE.getValue(), pathServers);

                // =================================================
                // ✅ VALIDACIONES
                // =================================================
                FileUtility.validatePaths(pathFile, pathLog, pathSwagger, pathServers);

                // =================================================
	            // ✅ BM_HOME PARA LOG4J
	            // =================================================
	            String bmHome = System.getenv("BM_HOME");
	
	            if (bmHome != null && !bmHome.trim().isEmpty()) {
	                System.setProperty("BM_HOME", bmHome);
	            }
	             
                // =================================================
                // ✅ LOG4J
                // =================================================
                Utils.setConfigLog4j(pathLog);

                // =================================================
                // ✅ LOG FINAL
                // =================================================
                log.info("APLICACION [{}] CONFIGURADA EXITOSAMENTE", BusinessCard.APP.getValue());

            }

        } catch (ExceptionFile e) {

            log.error("ERROR EN ARCHIVOS: {}", e.getMessage());
            System.exit(0);

        } catch (Exception e) {

            log.error("ERROR CARGANDO CONFIGURACION: {}", e.getMessage());
            System.exit(0);
        }
    }
}