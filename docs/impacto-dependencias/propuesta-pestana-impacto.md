# Propuesta: pestaña "Impacto / Dependencias" en Swagger Monitor

Basado en el código actual de `swagger-monitor` (Spring Boot + Thymeleaf, `dashboard.html`, Bootstrap 5.3 ya cargado, Chart.js ya cargado, `SwaggerMonitorController`, patrón de panels `.panel`).

## 0. Estado actual (punto de partida real)

`dashboard.html` hoy **no tiene pestañas**: es una sola página con paneles en grid (`panel-servers` = Servidores, luego Gráficas, Logs/Eventos, etc.). No hay que inventar un sistema de tabs desde cero: Bootstrap 5.3 ya está cargado, así que se puede usar `nav-tabs`/`tab-pane` nativo sin añadir librerías.

## 1. Distribución de paneles propuesta

Agregar una barra de pestañas arriba del `<div class="dashboard">` actual:

```
[ Monitoreo ]  [ Impacto / Dependencias ]
```

- **Tab "Monitoreo"**: contenido actual sin cambios (Servidores, Gráficas, Logs/Eventos).
- **Tab "Impacto / Dependencias"** (nueva), con sub-paneles en grid, de arriba a abajo:
  1. **Fila KPI (Dashboard Ejecutivo):** 5 tarjetas (`card text-center`, mismo estilo que las de Servidores): Dominios analizados, Servicios totales, Dependencias identificadas, Servicios críticos, SPOF detectados.
  2. **Fila dos columnas:**
     - Izquierda: **Ranking de Impacto** (lista ordenada / barra horizontal).
     - Derecha: **Matriz de Impacto** (tabla servicio × dominio, celdas coloreadas).
  3. **Fila Grafo de Dependencias** (ancho completo): grafo interactivo servicio ↔ dominios.
  4. **Panel lateral/modal: Detalle de Servicio** (reutilizando el patrón de modal `detailModal` que ya existe para servidores).
  5. **Vista por Dominio**: selector (`<select>`, mismo componente que `serverSelect`) que filtra ranking + matriz + grafo por el dominio elegido.

## 2. Gráficos recomendados

| Sección | Tipo de gráfico | Librería |
|---|---|---|
| Ranking de Impacto | Barras horizontales ordenadas por % | **Chart.js** (ya cargado, reutilizar patrón de `loadChartMulti`) |
| Matriz de Impacto | Tabla HTML con celdas coloreadas (heatmap discreto) | HTML/CSS puro + Bootstrap (no necesita librería de charts) |
| Grafo de Dependencias | Grafo nodo-enlace (servicio en el centro, dominios alrededor) | **vis-network** o **d3.js** — Chart.js no soporta grafos; añadir una sola librería ligera vía CDN |
| Dashboard Ejecutivo | Tarjetas KPI | Bootstrap `card` (sin librería adicional) |
| Badges de criticidad | Baja/Media/Alta/Crítica | Bootstrap `badge` con clases `bg-success/bg-warning/bg-danger` |

## 3. Componentes visuales

- **KPI cards**: igual estilo que las cards de servidor (`.card.text-center`), con número grande + etiqueta.
- **Heatmap/Matriz**: tabla `<table class="table table-sm">`, cada celda con fondo verde (0-20%), amarillo (21-60%) o rojo (61-100%), siguiendo la escala de 4 niveles del informe (Baja/Media/Alta/Crítica) colapsada a 3 colores visuales.
- **Grafo**: nodo central = servicio seleccionado (o microservicio `osba` completo), nodos periféricos = dominios; clic en un nodo de dominio → abre "Vista por Dominio"; clic en nodo de servicio → abre el modal de "Detalle de Servicio".
- **Ranking**: tabla u barras horizontales ordenadas desc. por `porcentajeAfectacion`, con badge de criticidad a la derecha de cada fila.
- **Modal de detalle** (reutilizar `#detailModal` o clonar su patrón): nombre, dominios consumidores, cantidad, %, criticidad, endpoint, dependencias relacionadas.
- **Selector de dominio**: `<select>` poblado con los dominios cargados (igual patrón que `serverSelect` + `loadChartMulti`).

## 4. Backend necesario (nuevo, no existe hoy)

No hay hoy ningún parser de `services.xml` / `restservices.xml` en `swagger-monitor` (el `ServerMonitorService` actual monitorea servidores, no XML de servicios). Se necesita:

- Un `ImpactoService` que, dado `BM_HOME`, recorra `appl/*/config/services.xml` (dominios) + el `restservices.xml` del microservicio objetivo, calcule la matriz igual que este informe (ver `afectacion.json` adjunto como contrato de datos de referencia) y cachee el resultado (reusar `CacheConfig`/`NoCacheConfig` existentes).
- Un `ImpactoController` con endpoints tipo:
  - `GET /api/impacto/dashboard` → KPIs.
  - `GET /api/impacto/servicios` → ranking completo.
  - `GET /api/impacto/servicio/{name}` → detalle.
  - `GET /api/impacto/dominio/{nombre}` → servicios/dependencias de ese dominio.
- Reutilizar el modelo de carpetas: igual que `ServerInfo`/`ServerHistory`, crear `ServiceImpact`/`DomainImpact`.

## 5. Archivos de referencia entregados en este análisis

- [`informe-impacto-dependencias.md`](./informe-impacto-dependencias.md) — resumen ejecutivo, tabla, hallazgos, inconsistencias de nomenclatura.
- [`afectacion.xml`](./afectacion.xml) — salida XML del análisis (servicios con coincidencia > 0%).
- [`afectacion.json`](./afectacion.json) — mismo resultado en JSON + metadatos, pensado como contrato de datos para `ImpactoService`/`ImpactoController`.

Estos tres archivos son el punto de partida para implementar la pestaña; la implementación real (parser dinámico, endpoints, HTML/JS de la nueva pestaña) se hace en el repo `SwaggerMonitor`.
