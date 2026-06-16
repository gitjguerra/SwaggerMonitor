package com.mercantilbanco.swaggermonitor.service;

import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.file.*;

@Service
public class SwaggerWatcherService {

    @PostConstruct
    public void startWatcher() {

        new Thread(() -> {

            try {

                String dir = getSwaggerDir();

                WatchService watchService =
                        FileSystems.getDefault().newWatchService();

                Path path = Paths.get(dir);

                // ✅ crear carpeta si no existe
                if (!path.toFile().exists()) {
                    path.toFile().mkdirs();
                }

                path.register(
                        watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY
                );

                System.out.println("👀 Watcher activo en: " + dir);

                while (true) {

                    WatchKey key = watchService.take();

                    for (WatchEvent<?> event : key.pollEvents()) {

                        Path fileName = (Path) event.context();

                        if (fileName.toString().endsWith(".json")) {

                            System.out.println(
                                    "🔥 Cambio detectado: "
                                            + event.kind().name()
                                            + " → "
                                            + fileName
                            );
                        }
                    }

                    key.reset();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }).start();
    }

    // =========================================================
    // ✅ OBTENER DIRECTORIO SWAGGER (OSBA)
    // =========================================================
    private String getSwaggerDir() {

        String dir = System.getProperty("pathSwaggerOutput");

        if (dir == null || dir.trim().isEmpty()) {
            throw new RuntimeException("❌ pathSwaggerOutput no configurado");
        }

        return dir.trim();
    }
}