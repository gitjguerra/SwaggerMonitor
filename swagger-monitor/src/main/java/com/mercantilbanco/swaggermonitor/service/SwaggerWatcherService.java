package com.mercantilbanco.swaggermonitor.service;

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

				String dir = System.getProperty("pathSwaggerOutput");

				if (dir == null || dir.trim().isEmpty()) {
					System.out.println("⚠️ Swagger watcher desactivado: no hay ruta configurada");
					return;
				}

				WatchService watchService = FileSystems.getDefault().newWatchService();

				Path path = Paths.get(dir.trim());

				if (!path.toFile().exists()) {
					path.toFile().mkdirs();
				}

				path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
						StandardWatchEventKinds.ENTRY_MODIFY);

				System.out.println("👀 Watcher activo en: " + dir);

				while (true) {

					WatchKey key = watchService.take();

					for (WatchEvent<?> event : key.pollEvents()) {

						Path fileName = (Path) event.context();

						if (fileName.toString().endsWith(".json")) {

							System.out.println("🔥 Cambio detectado: " + event.kind().name() + " → " + fileName);
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