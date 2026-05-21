package com.mercantilbanco.monitor.service;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Service
public class SlackAlertService {

	@Value("${slack.enabled:true}")
	private boolean slackEnabled;

    @Value("${slack.webhook.url}")
    private String webhookUrl;

    public void sendAlert(String message) {

		if (!slackEnabled) {
	        System.out.println("⚠️ Slack deshabilitado, no se envía alerta");
	        return;
	    }

        try {
            RestTemplate restTemplate = new RestTemplate();
            Map<String, String> body = new HashMap<>();

            String formattedMessage;

            formattedMessage = "Hola Usuario mercantil " + message;

            body.put("text", formattedMessage);

            String response = restTemplate.postForObject(webhookUrl, body, String.class);

            System.out.println("✅ Slack enviado correctamente: " + response);

        } catch (Exception e) {
            System.err.println("❌ Error enviando Slack");
            e.printStackTrace();
        }
    }
}