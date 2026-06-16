package com.mercantilbanco.swaggermonitor.util;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.mercantilbanco.swaggermonitor.exception.ExceptionFile;

/**
 * CLASE UTILITARIA ENCARGADA DE MANEJO DE ARCHIVOS.
 */
public class FileUtility {

    private FileUtility() {}

    /**
     * ✅ VALIDA QUE TODOS LOS PATH EXISTAN
     */
    public static void validatePaths(String... paths) throws ExceptionFile {

        for (int i = 0; i < paths.length; i++) {

            String path = paths[i];

            if (!isValidatePath(path)) {

                throw new ExceptionFile(
                        "❌ Path inválido o no existe [" + path + "] en posición " + (i + 1)
                );
            }
        }
    }

    /**
     * ✅ VALIDA EXISTENCIA DEL PATH
     */
    public static boolean isValidatePath(String path) {

        if (path == null || path.trim().isEmpty()) {
            return false;
        }

        try {

            Path tmpPath = Paths.get(path.trim());

            return tmpPath.toFile().exists();

        } catch (InvalidPathException e) {

            System.out.println("⚠️ Path inválido: " + path);
            return false;
        }
    }

    /**
     * ✅ VALIDA SI ES ARCHIVO
     */
    public static boolean isFile(Path path) {

        if (path == null) return false;

        return path.toFile().exists() && path.toFile().isFile();
    }

    /**
     * ✅ VALIDA SI ES DIRECTORIO
     */
    public static boolean isDirectory(Path path) {

        if (path == null) return false;

        return path.toFile().exists() && path.toFile().isDirectory();
    }
}