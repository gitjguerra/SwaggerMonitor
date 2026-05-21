# 🖥️ Server Monitor - Mercantil

Aplicación desarrollada en **Spring Boot** para monitorear el estado de servicios (TCP) definidos en un archivo XML y visualizar su estado en un dashboard web en tiempo real.

Incluye soporte para alertas vía Slack (opcional) y visualización con Thymeleaf.

---

# 🚀 Características

✅ Monitoreo de servidores por host + puerto  
✅ Lectura dinámica desde `servers.xml`  
✅ Dashboard web en tiempo real (auto refresh)  
✅ Cache con Caffeine para optimización  
✅ Alertas Slack configurables  
✅ Identificación de estado:

- 🟢 UP (Activo)
- 🔴 DOWN (Caído)

---

# 🏗️ Arquitectura
Controller → Service → XML Loader
→ Socket Check
→ Slack Alert
View (Thymeleaf Dashboard)

---

# 📂 Estructura del proyecto
src/main/java
├── controller
│     └── MonitorController.java
├── service
│     ├── ServerMonitorService.java
│     └── SlackAlertService.java
├── model
│     └── ServerInfo.java
├── config
│     └── CacheConfig.java
src/main/resources
├── templates
│     └── dashboard.html
├── static
│     └── images/mercantil.png
├── servers.xml
└── application.yaml

---

# ⚙️ Configuración

## application.yaml

```yaml
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
  enabled: false
  webhook:
    url: https://hooks.slack.com/services/XXXX

📡 Configuración de servidores (servers.xml)
<servers>

    <environment name="DEV">
        <server name="App1" host="127.0.0.1" port="8080" monitoring="true"/>
        <server name="DB1" host="127.0.0.1" port="3306" monitoring="true"/>
    </environment>

    <environment name="QA">
        <server name="AppQA" host="127.0.0.1" port="8081" monitoring="false"/>
    </environment>

</servers>

🔔 Slack Alerts
Activación

slack:
  enabled: true
  
Requisitos

Crear un Incoming Webhook en Slack
URL válida tipo:

https://hooks.slack.com/services/TXXXX/BXXXX/XXXXXXXX

Funcionamiento

Envía alerta cuando:
✅ cambia de estado (UP → DOWN o DOWN → UP)

🌐 Dashboard
Accede desde:  
http://localhost:8080

Características:

Actualización automática cada 10 segundos
Vista por entorno (DEV / QA / etc.)
Indicadores visuales
Diseño responsive

🧪 Ejecución
Compilar
mvn clean install

Ejecutar
mvn spring-boot:run

⚡ Tecnologías usadas

Java 8
Spring Boot 2.7.x
Thymeleaf
Bootstrap 5
Caffeine Cache
REST Template

⚠️ Problemas comunes
❌ Imagen no carga
➡ Mover a:
src/main/resources/static/images/


❌ Error Thymeleaf (TemplateInputException)
➡ Revisar:

etiquetas HTML mal cerradas
comillas en atributos (alt="...")
<link> mal formada


❌ Slack no envía
Verificar:

webhook válido
propiedad slack.enabled
salida a internet (firewall)


🚀 Mejoras futuras

✅ Health Check HTTP (status 200/500)
✅ Dashboard con gráficas
✅ Histórico de caídas
✅ Alertas por email / Teams
✅ Configuración dinámica sin reinicio


🧠 Autor
Proyecto interno de monitoreo desarrollado para uso corporativo.
  