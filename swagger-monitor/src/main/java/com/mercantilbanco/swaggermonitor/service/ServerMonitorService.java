package com.mercantilbanco.swaggermonitor.service;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.mercantilbanco.swaggermonitor.model.ServerHistory;
import com.mercantilbanco.swaggermonitor.model.ServerInfo;

@Service
public class ServerMonitorService {

	@Autowired
	private SlackAlertService slackAlertService;

	private Map<String, List<ServerInfo>> currentStatus = new HashMap<>();
	private Map<String, List<ServerInfo>> servers = new HashMap<>();

	// ✅ HISTÓRICO
	private Map<String, List<ServerHistory>> historyMap = new ConcurrentHashMap<>();

	// ✅ TIEMPO CAÍDO
	private Map<String, Date> downSince = new ConcurrentHashMap<>();

	private ExecutorService executor = Executors.newFixedThreadPool(10);

	@PostConstruct
	public void loadServers() {

		try {

			String pathFile = getServersFile();

			try (InputStream is = loadInputStream(pathFile)) {

				Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);

				NodeList envList = doc.getElementsByTagName("environment");

				for (int i = 0; i < envList.getLength(); i++) {

					Element env = (Element) envList.item(i);
					String envName = env.getAttribute("name");

					List<ServerInfo> list = new ArrayList<>();

					NodeList serverList = env.getElementsByTagName("server");

					for (int j = 0; j < serverList.getLength(); j++) {

						Element s = (Element) serverList.item(j);

						ServerInfo si = new ServerInfo();
						si.setName(s.getAttribute("name"));
						si.setHost(s.getAttribute("host"));
						si.setPort(Integer.parseInt(s.getAttribute("port")));

						String monitoringAttr = s.getAttribute("monitoring");
						si.setMonitoring("true".equalsIgnoreCase(monitoringAttr));

						si.setSlackWebhook(s.getAttribute("slackWebhook"));

						list.add(si);
					}

					servers.put(envName, list);
				}

				System.out.println("✅ Servers cargados: " + servers.size());

			}

		} catch (Exception e) {
			System.err.println("❌ Error cargando XML servidores");
			e.printStackTrace();
		}
	}

	@Cacheable("serverStatus")
	public Map<String, List<ServerInfo>> checkAll() {

		Map<String, List<ServerInfo>> result = new HashMap<>();

		for (String env : servers.keySet()) {

			List<Future<ServerInfo>> futures = new ArrayList<>();

			for (ServerInfo s : servers.get(env)) {

				futures.add(executor.submit(() -> processServer(s)));
			}

			List<ServerInfo> updated = new ArrayList<>();

			for (Future<ServerInfo> f : futures) {
				try {
					updated.add(f.get());
				} catch (Exception e) {
					System.err.println("Error en hilo");
					e.printStackTrace();
				}
			}

			result.put(env, updated);
		}

		currentStatus = result;
		return result;
	}

	private ServerInfo processServer(ServerInfo s) {

		long start = System.currentTimeMillis();

		boolean socketOk = isPortOpen(s);
		boolean alive = socketOk;

		if (socketOk) {
			long httpTime = checkServerTime(s.getHost(), s.getPort());
			alive = socketOk || httpTime >= 0;
		}

		long time = System.currentTimeMillis() - start;
		boolean previousStatus = s.isUp();

		s.setUp(alive);
		s.setResponseTime(time);
		s.setLastCheck(new Date());
		s.setSlow(time > 1000);

		// ✅ HISTÓRICO
		ServerHistory h = new ServerHistory();
		h.setName(s.getName());
		h.setUp(alive);
		h.setResponseTime(time);
		h.setTimestamp(new Date());

		historyMap.computeIfAbsent(s.getName(), k -> new CopyOnWriteArrayList<>());
		historyMap.get(s.getName()).add(h);

		if (historyMap.get(s.getName()).size() > 50) {
			historyMap.get(s.getName()).remove(0);
		}

		// ✅ TIEMPO CAÍDO
		if (!alive) {
			downSince.putIfAbsent(s.getName(), new Date());
		} else {
			downSince.remove(s.getName());
		}

		// ✅ ALERTA
		if (s.isMonitoring() && (s.isFirstCheck() || previousStatus != alive)) {

			String estado = alive ? "✅ RECUPERADO" : "🚨 CAIDO";

			String msg = "Servidor " + estado + ":\n" + "Nombre: " + s.getName() + "\n" + "Host: " + s.getHost() + ":"
					+ s.getPort();

			slackAlertService.sendAlert(s.getSlackWebhook(), msg);

			s.setFirstCheck(false);
		}

		return s;
	}

	// =========================================================
	// ✅ MÉTODOS PARA CONTROLLER
	// =========================================================

	public List<ServerHistory> getHistory(String name) {

		List<ServerHistory> list = historyMap.getOrDefault(name, new ArrayList<>());

		return new ArrayList<>(list);
	}

	public Map<String, List<ServerInfo>> getCurrentStatus() {
		return currentStatus;
	}

	public Map<String, List<ServerHistory>> getAllHistory() {
		return historyMap;
	}

	public double getUptime(String name) {

		List<ServerHistory> list = historyMap.get(name);

		if (list == null || list.size() < 2)
			return 100;

		long upCount = list.stream().filter(ServerHistory::isUp).count();

		return (upCount * 100.0) / list.size();
	}

	public String getDownTime(String name) {

		if (!downSince.containsKey(name))
			return "0 min";

		long diff = System.currentTimeMillis() - downSince.get(name).getTime();

		return (diff / (1000 * 60)) + " min";
	}

	// =========================================================
	// ✅ UTILIDADES
	// =========================================================
	private boolean isPortOpen(ServerInfo s) {

		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(s.getHost(), s.getPort()), 2000);
			s.setError("");
			return true;

		} catch (Exception e) {
			s.setError(e.getClass().getSimpleName());
			return false;
		}
	}

	public long checkServerTime(String host, int port) {

		try {

			long start = System.currentTimeMillis();

			URL url = new URL("http://" + host + ":" + port);

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(3000);
			conn.setReadTimeout(3000);

			int code = conn.getResponseCode();

			return (code >= 200 && code < 500) ? System.currentTimeMillis() - start : -1;

		} catch (Exception e) {
			return -1;
		}
	}

	private InputStream loadInputStream(String pathFile) {

		try {

			System.out.println("✅ Cargando servers.xml: " + pathFile);

			return new java.io.FileInputStream(pathFile.trim());

		} catch (Exception e) {

			System.out.println("⚠️ Usando servers.xml interno...");

			InputStream is = getClass().getClassLoader().getResourceAsStream("servers.xml");

			if (is == null) {
				throw new RuntimeException("❌ No se encontró servers.xml");
			}

			return is;
		}
	}

	private String getServersFile() {

		String path = System.getProperty("pathServersFile");

		if (path == null || path.trim().isEmpty()) {
			throw new RuntimeException("❌ pathServersFile no configurado");
		}

		return path.trim();
	}
}