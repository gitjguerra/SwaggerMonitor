package com.mercantilbanco.monitor.service;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.annotation.PostConstruct;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.mercantilbanco.monitor.model.ServerHistory;
import com.mercantilbanco.monitor.model.ServerInfo;

@Service
public class ServerMonitorService {

	@Autowired
	private SlackAlertService slackAlertService;

	@Value("${monitor.servers.file}")
	private String serversFile;

	private Map<String, List<ServerInfo>> currentStatus = new HashMap<>();
	private Map<String, List<ServerInfo>> servers = new HashMap<>();

	// ✅ HISTÓRICO
	private Map<String, List<ServerHistory>> historyMap = new HashMap<>();

	// ✅ TIEMPO CAÍDO
	private Map<String, Date> downSince = new HashMap<>();

	private ExecutorService executor = Executors.newFixedThreadPool(10);

	@SuppressWarnings("resource")
	@PostConstruct
	public void loadServers() {

		try {

			InputStream is;

			try {
				// ✅ intenta archivo externo
				is = new java.io.FileInputStream(serversFile);
				System.out.println("✅ Cargando servers.xml desde externo: " + serversFile);

			} catch (Exception e) {

				System.out.println("⚠️ No se encontró archivo externo servers.xml, usando interno...");

				is = getClass().getClassLoader().getResourceAsStream("servers.xml");

				if (is == null) {
					throw new RuntimeException("❌ No se encontró servers.xml en ningún lado");
				}
			}

			// ✅ try-with-resources
			try (InputStream autoClose = is) {

				Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(autoClose);

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

						list.add(si);
					}

					servers.put(envName, list);
				}
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

				futures.add(executor.submit(() -> {
					try {
						return processServer(s);
					} catch (Exception e) {
						System.err.println("💥 Error procesando: " + s.getName());
						e.printStackTrace();
						return s; // o null
					}
				}));

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

		boolean alive;

		boolean socketOk = isPortOpen(s);

		if (!socketOk) {
			alive = false;
		} else {
			// intenta HTTP, pero si falla igual sigue UP
			long httpTime = checkServerTime(s.getHost(), s.getPort());
			
			socketOk = isPortOpen(s);
			
			alive = socketOk || httpTime >= 0;			
		}

		long time = System.currentTimeMillis() - start;

		boolean previousStatus = s.isUp();

		s.setUp(alive);
		s.setResponseTime(time);
		s.setLastCheck(new Date());
		s.setSlow(time > 1000);

		// ✅ guardar SIEMPRE
		ServerHistory h = new ServerHistory();
		h.setName(s.getName());
		h.setUp(alive);
		h.setResponseTime(time);
		h.setTimestamp(new Date());

		historyMap.computeIfAbsent(s.getName(), k -> new ArrayList<>());
		historyMap.get(s.getName()).add(h);

		// limitar
		List<ServerHistory> list = historyMap.get(s.getName());
		if (list.size() > 50) {
			list.remove(0);
		}

		// ✅ TIEMPO CAÍDO
		if (!alive) {
			downSince.putIfAbsent(s.getName(), new Date());
		} else {
			downSince.remove(s.getName()); // 🔥 CRÍTICO
		}

		System.out.println(s.getName() + " -> " + (alive ? "UP" : "DOWN") + " | historial: "
				+ historyMap.getOrDefault(s.getName(), new ArrayList<>()).size());

		// ✅ ALERTA SLACK
		if (s.isMonitoring() && (s.isFirstCheck() || previousStatus != alive)) {

			String estado = alive ? "✅ RECUPERADO" : "🚨 CAIDO";

			String msg = "Servidor " + estado + ":\n" + "Nombre: " + s.getName() + "\n" + "Host: " + s.getHost() + ":"
					+ s.getPort();

			System.out.println("📢 Enviando alerta Slack:\n" + msg);

			slackAlertService.sendAlert(msg);

			s.setFirstCheck(false);
		}

		return s;
	}

	// ================================
	// ✅ MÉTODOS PARA EL FRONTEND
	// ================================

	public List<ServerHistory> getHistory(String name) {
		return historyMap.getOrDefault(name, new ArrayList<>());
	}

	public Map<String, List<ServerInfo>> getCurrentStatus() {
		return currentStatus;
	}

	private boolean isPortOpen(ServerInfo s) {

		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(s.getHost(), s.getPort()), 2000);

			s.setError(""); // ✅ sin error

			return true;

		} catch (java.net.SocketTimeoutException e) {

			s.setError("Timeout");
			return false;

		} catch (java.net.ConnectException e) {

			s.setError("Connection refused");
			return false;

		} catch (java.net.UnknownHostException e) {

			s.setError("Host desconocido");
			return false;

		} catch (Exception e) {

			s.setError("Error: " + e.getClass().getSimpleName());
			return false;
		}
	}

	public double getUptime(String name) {

		List<ServerHistory> list = historyMap.get(name);

		if (list == null || list.size() < 2)
			return 100; // ✅ FIX

		long upCount = list.stream().filter(ServerHistory::isUp).count();

		return (upCount * 100.0) / list.size();
	}

	public String getDownTime(String name) {

		if (!downSince.containsKey(name)) {
			return "0 min";
		}

		Date start = downSince.get(name);

		long diff = System.currentTimeMillis() - start.getTime();

		long minutes = diff / (1000 * 60);

		return minutes + " min";
	}

	public Map<String, List<ServerHistory>> getAllHistory() {
		return historyMap;
	}

	public long checkServerTime(String host, int port) {

		try {
			long start = System.currentTimeMillis();

			URL url = new URL("http://" + host + ":" + port);

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(3000);
			conn.setReadTimeout(3000);

			int code = conn.getResponseCode();

			long time = System.currentTimeMillis() - start;

			if (code >= 200 && code < 500) {
				return time;
			} else {
				return -1;
			}

		} catch (Exception e) {
			return -1;
		}
	}

}