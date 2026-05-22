package com.mercantilbanco.monitor.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
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

	@Autowired
	private ServerMonitorService service;

	@GetMapping("/")
	public String dashboard(Model model) {

		model.addAttribute("data", service.checkAll());

		return "dashboard";
	}

	// ✅ HISTÓRICO
	@GetMapping("/api/history/{name}")
	@ResponseBody
	public List<ServerHistory> getHistory(@PathVariable String name) {
		return service.getHistory(name);
	}

	// ✅ MÉTRICAS
	@GetMapping("/api/metrics/{name}")
	@ResponseBody
	public Map<String, Object> getMetrics(@PathVariable String name) {
		Map<String, Object> m = new HashMap<>();

		m.put("uptime", service.getUptime(name));
		m.put("downtime", service.getDownTime(name));

		return m;
	}

	@GetMapping("/api/alerts")
	@ResponseBody
	public List<Alert> getAlerts() {

		List<Alert> alerts = new ArrayList<>();

		Map<String, List<ServerInfo>> data = service.getCurrentStatus();

		if (data == null)
			return alerts;

		for (Map.Entry<String, List<ServerInfo>> env : data.entrySet()) {

			List<ServerInfo> servers = env.getValue();

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

	@GetMapping("/api/swagger/files")
	@ResponseBody
	public List<String> getSwaggerFiles() {

		List<String> result = new ArrayList<>();

		try {

			// ✅ ruta del proyecto
			String path = "src/main/resources/static/swagger";

			java.io.File folder = new java.io.File(path);

			if (!folder.exists() || !folder.isDirectory()) {
				return result;
			}

			for (java.io.File file : folder.listFiles()) {

				if (file.getName().endsWith(".json")) {

					// ✅ quitar .json
					String name = file.getName().replace(".json", "");

					result.add(name);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

}