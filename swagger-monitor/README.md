
# 🖥️ Server Monitor - Mercantil

![Java](https://img.shields.io/badge/Java-8-blue)
![SpringBoot](https://img.shields.io/badge/SpringBoot-2.7.x-brightgreen)
![Status](https://img.shields.io/badge/status-active-success)
![License](https://img.shields.io/badge/license-internal-red)
![Build](https://img.shields.io/badge/build-passing-success)

Aplicación desarrollada en **Spring Boot** para monitorear disponibilidad de servicios **TCP/HTTP**, con visualización en tiempo real y notificaciones inteligentes vía **Slack por servidor** Ademas de documentación swagger de los microservicios migrados a zconnect.

---

## 📊 Overview

**Server Monitor** permite centralizar el estado operativo de servicios distribuidos en distintos entornos (DEV, QA, PROD), ofreciendo:

- Visibilidad en tiempo real
- Alertas automáticas por canal específico
- Histórico de disponibilidad
- Arquitectura ligera y extensible
- Documentación swagger en linea de microservicios migrados de zconnect

---

## 🚀 Features
- ✅ Monitoreo TCP (host + puerto)
- ✅ Health check HTTP opcional
- ✅ Alertas Slack por servidor
- ✅ Dashboard web en tiempo real
- ✅ Caché con Caffeine
- ✅ Histórico de disponibilidad
- ✅ Detección de cambios de estado
- ✅ Multi-entorno (DEV, QA, etc.)
- ✅ documentación swagger de microservicios migrados zconnect

---

## 🏗️ Arquitectura

```text
                ┌────────────────────┐
                │   Controller       │
                └────────┬───────────┘
                         ↓
                ┌────────────────────┐
                │ ServerMonitorSvc   │
                └────────┬───────────┘
     ┌───────────────────┼─────────────────────┬────────────────────┐
     ↓                   ↓                     ↓		    ↓
 XML Loader        Socket Check         HTTP Check	Documentación Swagger
     ↓                                                              ↓
 Slack Alert Service		                        Microservicios Zconnect
     ↓
 Dashboard (Thymeleaf)
 
## 📂 Project Structure
	src/main/java
	├── controller
	│   └── MonitorController.java
	├── service
	│   ├── ServerMonitorService.java
	│   └── SlackAlertService.java
	├── model
	│   └── ServerInfo.java
	├── config
	│   └── CacheConfig.java

	src/main/resources
	├── templates
	│   └── dashboard.html
	├── static
	│   └── images/
	├── servers.xml
	└── application.yaml

## ⚙️ Configuration
   📌 application.yaml:   
		spring:
		  application:
		    name: Monitor
		  cache:
		    type: caffeine
		
		server:
		  port: 8080
		
		monitor:
		  refresh: 10
		
		slack:
		  enabled: true
  
## 📡 Servers Configuration (servers.xml)
      Cada servidor puede definir su propio canal Slack mediante un webhook:
      
<servers>

    <environment name="DEV">

        <server name="App1"
                host="127.0.0.1"
                port="8080"
                monitoring="true"
                slackWebhook="https://hooks.slack.com/services/AAA/111"/>

        <server name="DB1"
                host="127.0.0.1"
                port="3306"
                monitoring="true"
                slackWebhook="https://hooks.slack.com/services/BBB/222"/>

    </environment>

</servers>

## 🔔 Slack Notification System
   ✅ Comportamiento
      Las alertas se envían cuando:

🔴 UP → DOWN (caída)
🟢 DOWN → UP (recuperación)

## 📌 Características
	Envío por servidor (multi-canal)
	Tolerante a fallos (no rompe ejecución)
	Control global vía slack.enabled
	Evita spam (solo cambios de estado)

## 📌 Ejemplo de mensaje
	🚨 Servidor CAIDO:
	Nombre: App1
	Host: 127.0.0.1:8080

## 🌐 Dashboard
	Acceso:
	http://localhost:8080
	
## Características
	Auto-refresh configurable
	Vista por entorno
	Indicadores visuales (UP / DOWN)
	Diseño responsive


## 📈 Métricas disponibles
	✅ Uptime (%)
	✅ Tiempo de respuesta
	✅ Histórico reciente
	✅ Tiempo caído acumulado

## 🧪 Run
	Build	
	mvn clean install
	
	Run
	mvn spring-boot:run
	
## ⚡ Tech Stack
	-------------------------------------
	Tecnología			Uso
	-------------------------------------
	Java 8	            Backend base
	Spring Boot             Framework principal
	Thymeleaf	            Vista web
	Bootstrap 5             UI
	Caffeine                Cache
	RestTemplate	    Integración Slack
	
## ⚠️ Troubleshooting
	❌ Slack no envía
	✔ Verificar:

	slack.enabled=true
	Webhook válido en XML
	Conectividad externa	
	
## ❌ Servidor siempre DOWN
	✔ Revisar:

	Host accesible
	Puerto abierto
	Firewall
	
## ❌ Error Thymeleaf
	✔ Revisar:
		HTML mal formado
		Etiquetas sin cerrar
		Comillas en atributos


## 🔒 Seguridad
	⚠️ No exponer webhooks en repositorios públicos
	✅ Usar archivos externos para producción
	✅ Opcional: mapear canales en application.yaml
	
## 🚀 Roadmap
	Health check HTTP avanzado
	Gráficas en dashboard
	Persistencia histórica (DB)
	Alertas por Teams / Email
	Threshold de alertas (ej: >30s caído)
	Rate limiting Slack (anti-spam)
	Configuración dinámica sin restart
	
## 🧠 Author
	Gerencia de Servicios de Integración
	Mercantil Banco