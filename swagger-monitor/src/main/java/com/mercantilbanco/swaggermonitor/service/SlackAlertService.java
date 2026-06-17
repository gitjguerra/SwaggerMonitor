package com.mercantilbanco.swaggermonitor.service;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SlackAlertService {

	private static final Logger log = LogManager.getLogger(SlackAlertService.class);
	private boolean slackEnabled;

	// =========================================================
	// ✅ INIT OSBA
	// =========================================================
	@PostConstruct
	public void init() {

		String value = System.getProperty("slackEnabled");

		if (value == null || value.trim().isEmpty()) {
			slackEnabled = true; // ✅ default
		} else {
			slackEnabled = Boolean.parseBoolean(value);
		}
	}

	// =========================================================
	// ✅ SEND ALERT
	// =========================================================

	public void sendAlert(String webhookUrl, String message) {

		if (!slackEnabled) {
			log.warn("⚠️ Slack deshabilitado, no se envía alerta");
			return;
		}

		if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
			log.warn("⚠️ Servidor sin webhook configurado");
			return;
		}

		try {

			RestTemplate restTemplate = new RestTemplate();

			Map<String, String> body = new HashMap<>();

			String formattedMessage = "Hola Usuario mercantil " + message;

			body.put("text", formattedMessage);

			String response = restTemplate.postForObject(webhookUrl, body, String.class);

			log.info("✅ Slack enviado correctamente: {}", response);

		} catch (Exception e) {

			log.error("❌ Error enviando Slack", e); // ✅ incluye stacktrace correctamente
		}
	}

}