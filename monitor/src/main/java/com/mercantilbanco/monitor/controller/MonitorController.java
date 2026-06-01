
package com.mercantilbanco.monitor.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import com.mercantilbanco.monitor.model.Alert;
import com.mercantilbanco.monitor.model.ServerHistory;
import com.mercantilbanco.monitor.model.ServerInfo;
import com.mercantilbanco.monitor.service.ServerMonitorService;

@Controller
public class MonitorController {

	// ✅ carpeta externa configurada (producción)
	@Value("${monitor.swagger.external-dir:}")
	private String swaggerDir;

	@Autowired
	private ServerMonitorService service;

	// =========================================================
	// ✅ DASHBOARD
	// =========================================================
	@GetMapping("/")
	public String dashboard(Model model) {

		model.addAttribute("data", service.checkAll());
		return "dashboard";
	}

	// =========================================================
	// ✅ HISTÓRICO
	// =========================================================
	@GetMapping("/api/history/{name}")
	@ResponseBody
	public List<ServerHistory> getHistory(@PathVariable String name) {
		return service.getHistory(name);
	}

	// =========================================================
	// ✅ MÉTRICAS
	// =========================================================
	@GetMapping("/api/metrics/{name}")
	@ResponseBody
	public Map<String, Object> getMetrics(@PathVariable String name) {

		Map<String, Object> m = new HashMap<>();
		m.put("uptime", service.getUptime(name));
		m.put("downtime", service.getDownTime(name));

		return m;
	}

	// =========================================================
	// ✅ ALERTAS
	// =========================================================
	@GetMapping("/api/alerts")
	@ResponseBody
	public List<Alert> getAlerts() {

		List<Alert> alerts = new ArrayList<>();

		Map<String, List<ServerInfo>> data = service.getCurrentStatus();

		if (data == null)
			return alerts;

		for (List<ServerInfo> servers : data.values()) {

			if (servers == null)
				continue;

			for (ServerInfo srv : servers) {

				if (srv == null)
					continue;

				if (!srv.isUp()) {
					alerts.add(new Alert(srv.getName(), "DOWN", "Servidor caído", System.currentTimeMillis()));
				} else if (srv.getResponseTime() > 1000) {
					alerts.add(new Alert(srv.getName(), "SLOW", "Respuesta lenta (" + srv.getResponseTime() + " ms)",
							System.currentTimeMillis()));
				}

				if (srv.getError() != null && !srv.getError().isEmpty()) {
					alerts.add(new Alert(srv.getName(), "ERROR", srv.getError(), System.currentTimeMillis()));
				}
			}
		}

		return alerts;
	}

	// =========================================================
	// ✅ EVENTOS
	// =========================================================
	@GetMapping("/api/events")
	@ResponseBody
	public List<Map<String, Object>> getEvents() {

		List<Map<String, Object>> events = new ArrayList<>();

		Map<String, List<ServerHistory>> history = service.getAllHistory();
		if (history == null)
			return events;

		for (String server : history.keySet()) {

			List<ServerHistory> list = history.get(server);
			if (list == null)
				continue;

			for (ServerHistory h : list) {

				Map<String, Object> e = new HashMap<>();

				e.put("server", server);
				e.put("time", h.getTimestamp());

				if (!h.isUp()) {
					e.put("type", "DOWN");
					e.put("message", "Servidor se cayó");
				} else if (h.getResponseTime() > 1000) {
					e.put("type", "SLOW");
					e.put("message", "Se detectó lentitud");
				} else {
					e.put("type", "RECOVER");
					e.put("message", "Servidor recuperado");
				}

				events.add(e);
			}
		}

		return events;
	}

	// =========================================================
	// ✅ GRÁFICO POR SERVIDOR
	// =========================================================
	@GetMapping("/api/chart/{name}")
	@ResponseBody
	public Map<String, Object> getChartByServer(@PathVariable String name) {

		Map<String, Object> result = new HashMap<>();

		List<ServerHistory> list = service.getHistory(name);

		List<String> labels = new ArrayList<>();
		List<Long> values = new ArrayList<>();

		for (ServerHistory h : list) {
			labels.add(new java.text.SimpleDateFormat("HH:mm:ss").format(h.getTimestamp()));
			values.add(h.isUp() ? h.getResponseTime() : null);
		}

		result.put("labels", labels);
		result.put("values", values);

		return result;
	}

	// =========================================================
	// ✅ LISTA DE SERVIDORES
	// =========================================================
	@GetMapping("/api/servers")
	@ResponseBody
	public List<String> getServers() {

		List<String> list = new ArrayList<>();

		Map<String, List<ServerInfo>> data = service.getCurrentStatus();
		if (data == null)
			return list;

		for (List<ServerInfo> servers : data.values()) {
			for (ServerInfo s : servers) {
				list.add(s.getName());
			}
		}

		return list;
	}

	// =========================================================
	// ✅ LISTA DE SWAGGER DISPONIBLES
	// =========================================================

	@GetMapping("/api/swagger/files")
	public ResponseEntity<List<String>> getSwaggerFiles() {

		java.util.Set<String> result = new java.util.LinkedHashSet<>();

		try {

			// ✅ 1. DIRECTORIO EXTERNO
			if (swaggerDir != null && !swaggerDir.isEmpty()) {

				File folder = new File(swaggerDir);

				if (folder.exists() && folder.isDirectory()) {
					for (File file : folder.listFiles()) {
						if (file.getName().endsWith(".json")) {
							result.add(file.getName().replace(".json", "").toUpperCase());
						}
					}
				}
			}

			// ✅ 2. CLASSPATH
			org.springframework.core.io.ClassPathResource resource = new org.springframework.core.io.ClassPathResource(
					"static/swagger");

			if (resource.exists()) {
				try {
					File folder = resource.getFile();

					for (File file : folder.listFiles()) {
						if (file.getName().endsWith(".json")) {
							result.add(file.getName().replace(".json", "").toUpperCase());
						}
					}

				} catch (Exception ignored) {
					// ignore en jar
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		// ✅ 🔥 ORDEN ALFABÉTICO
		List<String> sorted = new ArrayList<>(result);
		java.util.Collections.sort(sorted);

		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Pragma", "no-cache")
				.header("Expires", "0").body(sorted);
	}

	// =========================================================
	// ✅ 🔥 ENDPOINT CLAVE PARA SWAGGER UI
	// =========================================================
	@GetMapping("/swagger/{name}.json")
	@ResponseBody
	public ResponseEntity<String> getSwagger(@PathVariable String name) {

		try {

			// ✅ EXTERNO
			if (swaggerDir != null && !swaggerDir.isEmpty()) {

				File file = findSwaggerFileIgnoreCase(swaggerDir, name);

				if (file != null && file.exists()) {

					String content = new String(java.nio.file.Files.readAllBytes(file.toPath()),
							java.nio.charset.StandardCharsets.UTF_8);

					return ResponseEntity.ok().header("Content-Type", "application/json;charset=UTF-8").body(content);
				}
			}

			// ✅ CLASSPATH
			org.springframework.core.io.ClassPathResource resource = new org.springframework.core.io.ClassPathResource(
					"static/swagger/" + name + ".json");

			if (resource.exists()) {

				java.io.InputStream is = resource.getInputStream();

				String content = readInputStream(is);

				return ResponseEntity.ok().header("Content-Type", "application/json;charset=UTF-8").body(content);
			}

			return ResponseEntity.notFound().build();

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().build();
		}
	}

	// =========================================================
	// ✅ UTILIDAD PARA LEER InputStream (Java 8 compatible)
	// =========================================================
	private String readInputStream(java.io.InputStream is) throws Exception {

		java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();

		int nRead;
		byte[] data = new byte[1024];

		while ((nRead = is.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}

		buffer.flush();

		return new String(buffer.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
	}

	private File findSwaggerFileIgnoreCase(String dir, String name) {

		File folder = new File(dir);

		if (!folder.exists() || !folder.isDirectory())
			return null;

		File[] files = folder.listFiles();

		if (files == null)
			return null;

		String target = name + ".json";

		for (File f : files) {
			if (f.getName().equalsIgnoreCase(target)) {
				return f;
			}
		}

		return null;
	}

}
