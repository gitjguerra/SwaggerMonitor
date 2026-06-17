package com.mercantilbanco.swaggermonitor.service;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import javax.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import com.mercantilbanco.swaggermonitor.exception.WatcherException;

@Service
public class SwaggerWatcherService {

	private static final Logger log = LogManager.getLogger(SwaggerWatcherService.class);

	@PostConstruct
	public void startWatcher() {

		// ✅ Sonar: usar method reference
		Thread watcherThread = new Thread(this::runWatcher);

		watcherThread.setName("swagger-watcher-thread");
		watcherThread.setDaemon(true);
		watcherThread.start();
	}

	// =========================================================
	// ✅ LÓGICA PRINCIPAL
	// =========================================================
	private void runWatcher() {

		try {

			String dir = getSwaggerDir();
			Path path = prepareDirectory(dir);

			WatchService watchService = FileSystems.getDefault().newWatchService();

			registerWatcher(path, watchService);

			log.info("👀 Watcher activo en: {}", dir);

			processEvents(watchService);

		} catch (IOException | InterruptedException e) {

			log.error("❌ Error en SwaggerWatcherService", e);

			// ✅ Sonar: excepción específica
			Thread.currentThread().interrupt();
		}
	}

	// =========================================================
	// ✅ PREPARAR DIRECTORIO
	// =========================================================
	private Path prepareDirectory(String dir) {

		Path path = Paths.get(dir);

		if (!path.toFile().exists()) {
			path.toFile().mkdirs();
			log.info("📁 Carpeta creada automáticamente: {}", dir);
		}

		return path;
	}

	// =========================================================
	// ✅ REGISTRAR WATCHER
	// =========================================================
	private void registerWatcher(Path path, WatchService watchService) throws IOException {

		if (path != null && watchService != null) { // ✅ invocación condicional

			path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
					StandardWatchEventKinds.ENTRY_MODIFY);

		} else {
			throw new WatcherException("❌ Path o WatchService inválido");
		}
	}

	// =========================================================
	// ✅ PROCESAR EVENTOS
	// =========================================================
	private void processEvents(WatchService watchService) throws InterruptedException {

		while (true) {

			WatchKey key = watchService.take();

			handleEvents(key);

			if (!key.reset()) {
				log.warn("⚠️ WatchKey inválido, deteniendo watcher");
				break;
			}
		}
	}

	// =========================================================
	// ✅ MANEJAR EVENTOS
	// =========================================================
	private void handleEvents(WatchKey key) {

		if (key == null) {
			log.warn("⚠️ WatchKey nulo recibido");
			return;
		}

		for (WatchEvent<?> event : key.pollEvents()) {

			Path fileName = (Path) event.context();

			if (isJsonFile(fileName)) {
				logEvent(event, fileName);
			}

		}
	}

	// =========================================================
	// ✅ VALIDACIÓN
	// =========================================================
	private boolean isJsonFile(Path fileName) {
		return fileName != null && fileName.toString().endsWith(".json");
	}

	// =========================================================
	// ✅ OBTENER DIRECTORIO SWAGGER
	// =========================================================
	private String getSwaggerDir() {

		String dir = System.getProperty("pathSwaggerOutput");

		if (dir == null || dir.trim().isEmpty()) {
			throw new WatcherException("❌ pathSwaggerOutput no configurado");
		}

		return dir.trim();
	}

	private void logEvent(WatchEvent<?> event, Path fileName) {

		if (event == null || event.kind() == null) {
			log.warn("⚠️ Evento inválido recibido");
			return;
		}

		String eventType = event.kind().name();

		log.info("🔥 Cambio detectado: {} → {}", eventType, fileName);
	}

}