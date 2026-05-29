package com.mercantilbanco.monitor.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.file.*;

@Service
public class SwaggerWatcherService {

    // ✅ ahora sí lee desde application.yaml
    @Value("${monitor.swagger.external-dir:}")
    private String dir;

    @PostConstruct
    public void startWatcher() {

        new Thread(() -> {

            try {

                // ✅ validar ruta
                if (dir == null || dir.trim().isEmpty()) {
                    System.out.println("⚠️ Swagger watcher de archivos OpenApi desactivado: no hay ruta configurada");
                    return;
                }

                WatchService watchService = FileSystems.getDefault().newWatchService();

                Path path = Paths.get(dir.trim());

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

                System.out.println("👀 Watcher activo de archivos OpenApi en: " + dir);

                while (true) {

                    WatchKey key = watchService.take();

                    for (WatchEvent<?> event : key.pollEvents()) {

                        WatchEvent.Kind<?> kind = event.kind();
                        Path fileName = (Path) event.context();

                        // ✅ solo archivos swagger
                        if (fileName.toString().endsWith(".json")) {

                            System.out.println("🔥 Cambio detectado: "
                                    + kind.name() + " → " + fileName);

                            // 👉 aquí luego puedes invalidar cache si implementas cache
                        }
                    }

                    key.reset();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }).start();
    }
}