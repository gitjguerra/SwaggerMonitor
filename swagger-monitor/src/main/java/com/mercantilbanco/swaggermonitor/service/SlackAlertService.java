package com.mercantilbanco.swaggermonitor.service;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SlackAlertService {

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
            System.out.println("⚠️ Slack deshabilitado, no se envía alerta");
            return;
        }

        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            System.out.println("⚠️ Servidor sin webhook configurado");
            return;
        }

        try {

            RestTemplate restTemplate = new RestTemplate();

            Map<String, String> body = new HashMap<>();

            String formattedMessage = "Hola Usuario mercantil " + message;

            body.put("text", formattedMessage);

            String response = restTemplate.postForObject(
                    webhookUrl,
                    body,
                    String.class
            );

            System.out.println("✅ Slack enviado correctamente: " + response);

        } catch (Exception e) {

            System.err.println("❌ Error enviando Slack");
            e.printStackTrace();
        }
    }
}