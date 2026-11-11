package com.mercantilbanco.swaggermonitor.controller;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mercantilbanco.swaggermonitor.exception.ControllerException;
import com.mercantilbanco.swaggermonitor.model.Alert;
import com.mercantilbanco.swaggermonitor.model.ServerHistory;
import com.mercantilbanco.swaggermonitor.model.ServerInfo;
import com.mercantilbanco.swaggermonitor.service.ImpactoService;
import com.mercantilbanco.swaggermonitor.service.ServerMonitorService;

@Controller
public class SwaggerMonitorController {

	private static final Logger log = LogManager.getLogger(SwaggerMonitorController.class);

	private final ServerMonitorService service;
	private final ImpactoService impactoService;
	private static final String MENSAJE = "message";
	private static final String JSON_EXTENSION = ".json";
	private static final String MSG_DOWN = "Servidor se cayó";
	private static final String MSG_SLOW = "Se detectó lentitud";
	private static final String MSG_RECOVER = "Servidor recuperado";

	private static final String TYPE_DOWN = "DOWN";
	private static final String TYPE_SLOW = "SLOW";
	private static final String TYPE_RECOVER = "RECOVER";

	// ✅ Constructor Injection
	public SwaggerMonitorController(ServerMonitorService service, ImpactoService impactoService) {
		this.service = service;
		this.impactoService = impactoService;
	}

	@GetMapping("/")
	public String dashboard(Model model) {
		model.addAttribute("data", service.checkAll());
		return "dashboard";
	}

	@ResponseBody
	@GetMapping("/api/history/{name}")
	public List<ServerHistory> getHistory(@PathVariable String name) {
		return service.getHistory(name);
	}

	@ResponseBody
	@GetMapping("/api/metrics/{name}")
	public Map<String, Object> getMetrics(@PathVariable String name) {

		Map<String, Object> m = new HashMap<>();
		m.put("uptime", service.getUptime(name));
		m.put("downtime", service.getDownTime(name));
		return m;
	}

	@ResponseBody
	@GetMapping("/api/alerts")
	public List<Alert> getAlerts() {

		List<Alert> alerts = new ArrayList<>();

		Map<String, List<ServerInfo>> data = service.getCurrentStatus();

		if (data == null) {
			return alerts;
		}

		for (List<ServerInfo> servers : data.values()) {

			if (servers == null)
				continue;

			for (ServerInfo srv : servers) {

				processServerAlert(srv, alerts);
			}
		}

		return alerts;
	}

	@ResponseBody
	@GetMapping("/api/events")
	public List<Map<String, Object>> getEvents() {

		List<Map<String, Object>> events = new ArrayList<>();
		Map<String, List<ServerHistory>> history = service.getAllHistory();

		if (history == null)
			return events;

		for (Map.Entry<String, List<ServerHistory>> entry : history.entrySet()) {

			String server = entry.getKey();
			List<ServerHistory> list = entry.getValue();

			if (list == null)
				continue;

			for (ServerHistory h : list) {

				if (h == null)
					continue;

				Map<String, Object> e = new HashMap<>();
				e.put("server", server);
				e.put("time", h.getTimestamp());

				if (!h.isUp()) {
					e.put("type", TYPE_DOWN);
					e.put(MENSAJE, MSG_DOWN);
				} else if (h.getResponseTime() > 1000) {
					e.put("type", TYPE_SLOW);
					e.put(MENSAJE, MSG_SLOW);
				} else {
					e.put("type", TYPE_RECOVER);
					e.put(MENSAJE, MSG_RECOVER);
				}

				events.add(e);
			}
		}

		return events;
	}

	@ResponseBody
	@GetMapping("/api/chart/{name}")
	public Map<String, Object> getChartByServer(@PathVariable String name) {

		Map<String, Object> result = new HashMap<>();

		List<ServerHistory> rawList = service.getHistory(name);
		List<ServerHistory> listSafe = (rawList != null) ? new ArrayList<>(rawList) : new ArrayList<>();

		List<String> labels = new ArrayList<>();
		List<Long> values = new ArrayList<>();

		if (listSafe.isEmpty()) {
			result.put("labels", labels);
			result.put("values", values);
			return result;
		}

		for (ServerHistory h : listSafe) {

			if (h == null)
				continue;

			labels.add(new SimpleDateFormat("HH:mm:ss").format(h.getTimestamp()));
			values.add(h.isUp() ? h.getResponseTime() : 1800);
		}

		result.put("labels", labels);
		result.put("values", values);

		return result;
	}

	@ResponseBody
	@GetMapping("/api/servers")
	public List<String> getServers() {

		List<String> list = new ArrayList<>();
		Map<String, List<ServerInfo>> data = service.getCurrentStatus();

		if (data == null)
			return list;

		for (List<ServerInfo> servers : data.values()) {
			if (servers == null)
				continue;

			for (ServerInfo s : servers) {
				list.add(s.getName());
			}
		}

		return list;
	}

	@ResponseBody
	@GetMapping("/api/swagger/files")
	public ResponseEntity<List<String>> getSwaggerFiles() {

		Set<String> result = new LinkedHashSet<>();

		try {

			String swaggerDir = getSwaggerDir();
			File folder = new File(swaggerDir);

			if (folder.exists() && folder.isDirectory()) {

				File[] files = folder.listFiles();

				if (files != null) {
					for (File file : files) {
						if (file.getName().endsWith(JSON_EXTENSION)) {
							result.add(file.getName().replace(JSON_EXTENSION, "").toUpperCase());
						}
					}
				}
			}

		} catch (Exception e) {
			log.error("❌ Error obteniendo archivos swagger", e);
			throw new ControllerException("Error leyendo archivos swagger", e);
		}

		List<String> sorted = new ArrayList<>(result);
		Collections.sort(sorted);

		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Pragma", "no-cache")
				.header("Expires", "0").body(sorted);
	}

	@ResponseBody
	@GetMapping("/swagger/{name}.json")
	public ResponseEntity<String> getSwagger(@PathVariable String name) {

		try {

			String swaggerDir = getSwaggerDir();
			File file = findSwaggerFileIgnoreCase(swaggerDir, name);

			if (file != null && file.exists()) {

				String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
				content = anotarNombresDePrograma(content, name);

				return ResponseEntity.ok().header("Content-Type", "application/json;charset=UTF-8").body(content);
			}

			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

		} catch (Exception e) {
			log.error("❌ Error obteniendo swagger", e);
			throw new ControllerException("Error leyendo swagger", e);
		}
	}

	private static final Set<String> METODOS_HTTP = new HashSet<>(java.util.Arrays.asList(
			"get", "post", "put", "delete", "patch", "options", "head", "trace"));

	// 17 espacios de separación antes de "programa: ", a pedido de negocio.
	private static final String SEPARADOR_PROGRAMA = "                 programa: ";
	private static final String PREFIJO_PROGRAMA = "programa: ";

	/**
	 * Agrega al "summary" de cada operación del swagger el nombre de
	 * programa que la implementa. El path del swagger (p. ej.
	 * "/actualizar-roles") no contiene ese dato: el nombre de programa
	 * real vive en el atributo "endpoint" del services.xml de ESE
	 * MISMO dominio (appl/&lt;dominio&gt;/config/services.xml, p. ej.
	 * ".../actualizarRoles-gipw043x"), bajo el mismo "name" que el
	 * path del swagger. Por eso se cruza cada path contra el name de
	 * ese dominio (no contra restservices.xml de osba) y se reutiliza
	 * el programa ya resuelto por {@link ImpactoService}. Si algo
	 * falla, se devuelve el contenido original sin modificar en vez de
	 * romper la documentación.
	 */
	private String anotarNombresDePrograma(String swaggerJson, String dominio) {

		try {

			Map<String, String> programasPorNombre = impactoService.getProgramasDominio(dominio);

			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(swaggerJson);
			JsonNode paths = root.path("paths");

			if (!paths.isObject()) {
				return swaggerJson;
			}

			Iterator<Map.Entry<String, JsonNode>> pathFields = paths.fields();

			while (pathFields.hasNext()) {

				Map.Entry<String, JsonNode> pathEntry = pathFields.next();
				String programa = buscarPrograma(pathEntry.getKey(), programasPorNombre);

				if (programa == null || !pathEntry.getValue().isObject()) {
					continue;
				}

				Iterator<Map.Entry<String, JsonNode>> methodFields = pathEntry.getValue().fields();

				while (methodFields.hasNext()) {

					Map.Entry<String, JsonNode> methodEntry = methodFields.next();

					if (!METODOS_HTTP.contains(methodEntry.getKey().toLowerCase())
							|| !methodEntry.getValue().isObject()) {
						continue;
					}

					ObjectNode operacion = (ObjectNode) methodEntry.getValue();
					String resumenActual = operacion.path("summary").asText("");

					String nuevoResumen = resumenActual.trim().isEmpty()
							? PREFIJO_PROGRAMA + programa
							: resumenActual + SEPARADOR_PROGRAMA + programa;

					operacion.put("summary", nuevoResumen);
				}
			}

			return mapper.writeValueAsString(root);

		} catch (Exception e) {
			log.warn("⚠️ No se pudo anotar el swagger con el nombre de programa, se sirve sin modificar", e);
			return swaggerJson;
		}
	}

	/**
	 * Busca, entre los segmentos del path del swagger (ignorando
	 * parámetros tipo "{id}"), uno que coincida (sin distinguir
	 * mayúsculas/minúsculas) con el "name" de algún servicio de
	 * restservices.xml, y devuelve su programa ya resuelto.
	 */
	private String buscarPrograma(String path, Map<String, String> programasPorNombre) {

		if (path == null) {
			return null;
		}

		String limpio = path.trim();
		int queryIdx = limpio.indexOf('?');
		if (queryIdx >= 0) {
			limpio = limpio.substring(0, queryIdx);
		}

		for (String segmento : limpio.split("/")) {

			if (segmento.isEmpty() || (segmento.startsWith("{") && segmento.endsWith("}"))) {
				continue;
			}

			String programa = programasPorNombre.get(segmento.toLowerCase());
			if (programa != null) {
				return programa;
			}
		}

		return null;
	}

	private File findSwaggerFileIgnoreCase(String dir, String name) {

		File folder = new File(dir);

		if (!folder.exists() || !folder.isDirectory())
			return null;

		File[] files = folder.listFiles();
		if (files == null)
			return null;

		String target = name + JSON_EXTENSION;

		for (File f : files) {
			if (f.getName().equalsIgnoreCase(target)) {
				return f;
			}
		}

		return null;
	}

	private String getSwaggerDir() {

		String dir = System.getProperty("pathSwaggerOutput");

		if (dir == null || dir.trim().isEmpty()) {
			throw new ControllerException("❌ pathSwaggerOutput no configurado");
		}

		return dir;
	}

	@ResponseBody
	@GetMapping("/api/servers/status")
	public List<Map<String, Object>> getServersStatus() {

		List<Map<String, Object>> result = new ArrayList<>();
		Map<String, List<ServerInfo>> data = service.getCurrentStatus();

		if (data == null)
			return result;

		for (List<ServerInfo> servers : data.values()) {
			if (servers == null)
				continue;

			for (ServerInfo srv : servers) {

				Map<String, Object> m = new HashMap<>();
				m.put("name", srv.getName());
				m.put("up", srv.isUp());
				m.put("responseTime", srv.getResponseTime());

				result.add(m);
			}
		}

		return result;
	}

	private void processServerAlert(ServerInfo srv, List<Alert> alerts) {

		if (srv == null)
			return;

		long now = System.currentTimeMillis();

		if (!srv.isUp()) {
			alerts.add(new Alert(srv.getName(), "DOWN", "Servidor caído", now));
			return;
		}

		if (srv.getResponseTime() > 1000) {
			alerts.add(new Alert(srv.getName(), "SLOW", "Respuesta lenta (" + srv.getResponseTime() + " ms)", now));
		}

		if (hasError(srv)) {
			alerts.add(new Alert(srv.getName(), "ERROR", srv.getError(), now));
		}
	}

	private boolean hasError(ServerInfo srv) {
		return srv.getError() != null && !srv.getError().isEmpty();
	}

}