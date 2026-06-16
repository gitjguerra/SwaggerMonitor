package com.mercantilbanco.swaggermonitor.config;

import lombok.Getter;

@Getter
public enum BusinessCard {

    // =========================================================
    // 🔹 BASE
    // =========================================================

    BM_HOME("BM_HOME"),
    SEPARATOR(System.getProperty("file.separator")),
    APP("swagger-monitor"),

    // =========================================================
    // 🔹 CONFIG PRINCIPAL
    // =========================================================

    CONFIG("swagger-monitor.xml"),
    TEST_ENVIRONMENT("testEnvironment"),

    // =========================================================
    // 🔹 PATHS
    // =========================================================

    LOG4J("pathLog4jFile"),
    SWAGGER_OUTPUT("pathSwaggerOutput"),
    SERVERS_FILE("pathServersFile"),

    // =========================================================
    // 🔹 MONITOR
    // =========================================================

    REFRESH("monitorRefresh"),
    SLACK_ENABLED("slackEnabled"),
	SLACK_WEBHOOK("slackWebhookUrl");
	
    private final String value;

    BusinessCard(String value) {
        this.value = value;
    }
}