package com.mercantilbanco.swaggermonitor.util;

import java.io.File;
import java.nio.file.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import com.mercantilbanco.swaggermonitor.exception.ExceptionFile;

/**
 * CLASE UTILITARIA PARA LA APLICACION.
 * <hr>
 * <code>swagger-generator - Banco Mercantil</code>
 */
public class Utils {

    /** The Constant SEPARATOR. */
    public static final String SEPARATOR = System.getProperty("file.separator");

    /** The Constant BM_HOME. */
    private static final String BM_HOME = "BM_HOME";

    private Utils() {}

    /**
     * METODO ENCARGADO DE RETORNAR LA RUTA DEL ARCHIVO DE CONFIGURACION.
     * @param appName the app name
     * @return the file app config
     */
    public static String getFileAppConfig(String appName) {

        StringBuilder path = new StringBuilder();

        return path
                .append(getPathHome(appName))
                .append(appName)
                .append(".xml")
                .toString();
    }

    /**
     * METODO ENCARGADO DE RETORNAR LA RUTA BASE DE LA APLICACION.
     */
    private static String getPathHome(String appName) {

        return new StringBuilder()
                .append(System.getenv(BM_HOME))
                .append(SEPARATOR)
                .append("appl")
                .append(SEPARATOR)
                .append(appName)
                .append(SEPARATOR)
                .append("config")
                .append(SEPARATOR)
                .toString();
    }

    /**
     * METODO ENCARGADO DE RESOLVER UNA RUTA.
     */
    public static String resolvePath(String value) {

        if (value == null || value.trim().isEmpty())
            return value;

        return value
                .replace("${BM_HOME}", System.getenv(BM_HOME))
                .replace("${env:BM_HOME}", System.getenv(BM_HOME));
    }

    /**
     * METODO ENCARGADO DE CONFIGURAR LOS TAGS.
     */
    public static void setListtags(Path pathFile, boolean test) {
        // mantener igual OSBA aunque no se use aún
    }

    /**
     * Extrae el "nombre de programa" de un endpoint: la última
     * palabra del último segmento de la ruta, separada por "-"
     * (p. ej. para ".../consultarTelefonosDetallado-gipw106x" devuelve
     * "gipw106x"). Devuelve null si el endpoint es vacío o si el
     * último segmento no tiene un "-" (no hay nombre de programa que
     * extraer).
     */
    public static String extraerNombrePrograma(String endpoint) {

        if (endpoint == null || endpoint.trim().isEmpty()) {
            return null;
        }

        String limpio = endpoint.trim();

        int queryIdx = limpio.indexOf('?');
        if (queryIdx >= 0) {
            limpio = limpio.substring(0, queryIdx);
        }

        while (limpio.endsWith("/")) {
            limpio = limpio.substring(0, limpio.length() - 1);
        }

        int slashIdx = limpio.lastIndexOf('/');
        String segmento = (slashIdx >= 0) ? limpio.substring(slashIdx + 1) : limpio;

        int hyphenIdx = segmento.lastIndexOf('-');

        if (hyphenIdx < 0 || hyphenIdx == segmento.length() - 1) {
            return null;
        }

        return segmento.substring(hyphenIdx + 1);
    }

    /**
     * METODO ENCARGADO DE CONFIGURAR LOG4J.
     */
    public static void setConfigLog4j(String path) throws ExceptionFile {

        // ✅ VALIDACION
        if (!FileUtility.isValidatePath(path)) {
            throw new ExceptionFile("NO ESTA EL ARCHIVO [" + path + "]");
        }

        File file = new File(path);

        // ✅ LOG4J2 REAL (OSBA STYLE)
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.setConfigLocation(file.toURI());

        LogManager.getLogger(Utils.class)
                .info("LOG4J INICIADO EN: {}", path);
    }
}