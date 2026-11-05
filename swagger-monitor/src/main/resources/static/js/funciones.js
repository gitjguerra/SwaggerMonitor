const BASE_PATH = window.location.pathname.startsWith('/swagger-monitor') ? '/swagger-monitor' : '';

function api(path) {
    return BASE_PATH + path;
}

// ======================================================
// ✅ MODAL DETALLE SERVIDOR
// ======================================================
window.showDetails = function(name, host, port, up, time, error) {

    document.getElementById("modalName").innerText = name;
    document.getElementById("modalHost").innerText = host;
    document.getElementById("modalPort").innerText = port;
    document.getElementById("modalTime").innerText = time;
    document.getElementById("modalError").innerText = error ? error : "Sin error";

    let statusElement = document.getElementById("modalStatus");

    if (up === 'true') {
        if (time > 1000) {
            statusElement.innerText = 'LENTO';
            statusElement.className = 'badge bg-warning';
        } else {
            statusElement.innerText = 'ACTIVO';
            statusElement.className = 'badge bg-success';
        }
    } else {
        statusElement.innerText = 'INACTIVO';
        statusElement.className = 'badge bg-danger';
    }

    // ✅ MÉTRICAS
    fetch('/api/metrics/' + name)
        .then(res => res.json())
        .then(data => {
            if (data.uptime >= 99.9) {
                document.getElementById("modalUptime").innerText = "100% (estable)";
            } else if (data.uptime === 0) {
                document.getElementById("modalUptime").innerText = "0% (caído)";
            } else {
                document.getElementById("modalUptime").innerText =
                    data.uptime.toFixed(2) + "%";
            }

            document.getElementById("modalDownTime").innerText = data.downtime;
        });

    // ✅ HISTÓRICO
    fetch('/api/history/' + name)
        .then(res => res.json())
        .then(data => {

            let table = document.getElementById("historyTable");
            table.innerHTML = "";

            data.slice(-10).reverse().forEach(h => {

                let d = new Date(h.timestamp);
                let time = isNaN(d) ? h.timestamp : d.toLocaleTimeString();

                table.innerHTML += `
                    <tr>
                        <td>${time}</td>
                        <td>
                            <span class="badge ${h.up ? 'bg-success' : 'bg-danger'}">
                                ${h.up ? 'UP' : 'DOWN'}
                            </span>
                        </td>
                        <td>${h.responseTime} ms</td>
                    </tr>
                `;
            });
        });

    new bootstrap.Modal(document.getElementById('detailModal')).show();
};

// ======================================================
// ✅ ALERTAS
// ======================================================
function loadAlerts() {

    fetch('/api/alerts')
        .then(res => res.json())
        .then(data => {

            let container = document.getElementById("alertsContainer");
            container.innerHTML = "";

            if (data.length === 0) {
                container.innerHTML = "<p class='text-success'>✅ Sin alertas</p>";
                return;
            }

            data.forEach(alert => {

                let color = "secondary";
                if (alert.level === "DOWN") color = "danger";
                if (alert.level === "SLOW") color = "warning";
                if (alert.level === "ERROR") color = "dark";

                let time = new Date(alert.timestamp).toLocaleTimeString();

                container.innerHTML += `
                    <div class="card mb-2 border-${color}">
                        <div class="card-body p-2">
                            <strong>${alert.server}</strong>
                            <span class="badge bg-${color}">${alert.level}</span>
                            <div class="small-text">${alert.message}</div>
                            <div class="small-text">${time}</div>
                        </div>
                    </div>
                `;
            });
        });
}

// ======================================================
// ✅ EVENTOS (TIMELINE)
// ======================================================
function loadEvents() {

    fetch('/api/events')
        .then(res => res.json())
        .then(data => {

            let container = document.getElementById("eventsContainer");
            container.innerHTML = "";

            if (data.length === 0) {
                container.innerHTML = "<p>Sin eventos</p>";
                return;
            }

            data.slice(-15).reverse().forEach(e => {

                let colorClass = "dot-secondary";
                let icon = "ℹ️";

                if (e.type === "DOWN") {
                    colorClass = "dot-danger";
                    icon = "🔴";
                } else if (e.type === "SLOW") {
                    colorClass = "dot-warning";
                    icon = "🟡";
                } else if (e.type === "RECOVER") {
                    colorClass = "dot-success";
                    icon = "✅";
                }

                let time = new Date(e.time).toLocaleTimeString();

                container.innerHTML += `
                    <div class="timeline-item">
                        <div class="timeline-dot ${colorClass}"></div>
                        <div class="timeline-content">
                            <strong>${icon} ${e.server}</strong>
                            <div class="small-text">${e.message}</div>
                            <div class="small-text">${time}</div>
                        </div>
                    </div>
                `;
            });
        });
}

// ======================================================
// ✅ GRÁFICA MULTI SERVIDOR
// ======================================================

	let chart = null;
	let historyData = {};   // ✅ persistente
	const MAX_POINTS = 20;

	// 🎨 paleta fija de colores
	const COLORS = [
	    "#3b82f6", // azul
	    "#ef4444", // rojo
	    "#10b981", // verde
	    "#f59e0b", // naranja
	    "#8b5cf6", // morado
	    "#14b8a6", // teal
	    "#e11d48", // rosado
	    "#6366f1", // índigo
	    "#84cc16", // lima
	    "#f97316"  // naranja fuerte
	];

	function loadChartMulti() {

	    const select = document.getElementById("serverSelect");
	    const selected = Array.from(select.selectedOptions).map(o => o.value);

	    if (selected.length === 0) return;

	    Promise.all(
	        selected.map(name =>
	            fetch('/api/chart/' + name).then(r => r.json())
	        )
	    )
	    .then(results => {

	        const ctx = document.getElementById("responseChart");

	        if (chart) chart.destroy();

	        let labels = [];
	        let datasets = [];

	        results.forEach((res, idx) => {

	            const name = selected[idx];

	            if (!historyData[name]) {
	                historyData[name] = [];
	            }

	            let value = 0;

	            if (res.values && res.values.length > 0) {
	                value = res.values[res.values.length - 1];
	            }

	            const base = idx * 2;   // 👈 altura del carril

	            const isDown = value >= 1700;
	            const isSlow = value > 1000 && value < 1700;

	            let scaledValue;

	            if (isDown) {
	                scaledValue = base;          // 🔴 abajo
	            } else if (isSlow) {
	                scaledValue = base + 0.5;    // 🟡 medio
	            } else {
	                scaledValue = base + 1;      // 🟢 arriba
	            }

	            historyData[name].push(scaledValue);

	            // mantener tamaño fijo
	            if (historyData[name].length > MAX_POINTS) {
	                historyData[name].shift();
	            }

	            // eje X simple (0..n)
	            labels = historyData[name].map((_, i) => i);

	            let color = COLORS[idx % COLORS.length];

	            datasets.push({
	                label: name,
	                data: historyData[name],
	                borderColor: color,
	                fill: false,
	                tension: 0.2,
	                pointRadius: 2
	            });

	        });

	        chart = new Chart(ctx, {
	            type: 'line',
	            data: {
	                labels: labels,
	                datasets: datasets
	            },
	            options: {
	                animation: false,
	                responsive: true,
	                maintainAspectRatio: false,
	                scales: {
	                	y: {
	                	    min: 0,
	                	    max: selected.length * 2,
	                	    ticks: {
	                	        stepSize: 1,
	                	        callback: function(value) {

	                	            const index = Math.floor(value / 2);

	                	            if (index >= selected.length) return "";

	                	            const serverName = selected[index];

	                	            return value % 2 === 1 
	                	                ? serverName + " ↑"   // UP
	                	                : serverName + " ↓";  // DOWN
	                	        }
	                	    },
	                	    grid: {   // ✅ fuera de ticks
	                	        color: ctx => {
	                	            return ctx.tick.value % 2 === 0 
	                	                ? "#e5e7eb"
	                	                : "#ffffff";
	                	        }
	                	    }
	                	}
	                }
	            }
	        });

	    })
	    .catch(err => console.error("Chart error", err));
	}

// ======================================================
// ✅ SELECTOR SERVIDORES
// ======================================================
function loadServerSelector() {

    fetch('/api/servers', { cache: "no-store" })
        .then(res => res.json())
        .then(data => {

            let select = document.getElementById("serverSelect");
            select.innerHTML = "";

            data.forEach(name => {
                let option = document.createElement("option");
                option.value = name;
                option.textContent = name;
                select.appendChild(option);
            });

            // seleccionar por defecto
            for (let i = 0; i < Math.min(2, select.options.length); i++) {
                select.options[i].selected = true;
            }

            loadChartMulti();
        });
}

// ======================================================
// ✅ DASHBOARD REFRESH
// ======================================================
function refreshDashboard() {
    location.reload();   // 🔥 fuerza actualización total
}


// ======================================================
// ✅ INIT
// ======================================================

document.addEventListener("DOMContentLoaded", function () {

    loadServerSelector();

    // ✅ ✅ ESTA ES LA SOLUCIÓN
    loadEvents();
    loadAlerts();

    // ✅ REFRESH AUTOMÁTICO (recomendado)
    setInterval(loadEvents, 5000);
    setInterval(loadAlerts, 5000);
    setInterval(loadChartMulti, 5000);
    setInterval(updateCards, 5000);
    
    document.getElementById("serverSelect")
        .addEventListener("change", loadChartMulti);

});


//======================================================
//✅ SWAGGER
//======================================================
async function openSwaggerPanel() {

    const panel = document.getElementById("swaggerPanel");
    panel.style.display = "block";

    let select = document.getElementById("apiSelect");
    select.innerHTML = "";

    // ✅ cargar dinámicamente
    const swaggerApis = await loadSwaggerApis();

    if (swaggerApis.length === 0) {
        select.innerHTML = "<option>No hay archivos</option>";
        return;
    }

    swaggerApis.forEach(api => {
        let opt = document.createElement("option");
        opt.value = api;
        opt.textContent = api.toUpperCase();
        select.appendChild(opt);
    });

    select.onchange = function () {
        loadSwaggerUI(this.value);
    };

    // ✅ cargar primero automáticamente
    loadSwaggerUI(swaggerApis[0]);

    panel.scrollIntoView({ behavior: "smooth" });
}


function loadSwaggerUI(apiName) {
	
    const swaggerUrl =
        window.location.origin +
        "/swagger/" +
        apiName +
        ".json";

    document.getElementById("swaggerUI").innerHTML = "";

    fetch(swaggerUrl)
        .then(r => r.json())
        .then(spec => {

            SwaggerUIBundle({

                spec: spec,

                dom_id: "#swaggerUI",

                deepLinking: true,

                presets: [
                    SwaggerUIBundle.presets.apis
                ],

                supportedSubmitMethods: [],

                layout: "BaseLayout"
            });

        });
}


async function loadSwaggerApis() {

    try {
    	const res = await fetch('/api/swagger/files', { cache: "no-store" });
        const data = await res.json();

        console.log("📘 swaggerApis RAW:", data);

        // ✅ FIX CRÍTICO
        if (!Array.isArray(data)) {
            console.error("❌ No es array:", data);
            return [];
        }

        return data;

    } catch (e) {
        console.error("Error cargando APIs:", e);
        return [];
    }
}


function updateCards() {

    fetch('/api/servers/status') // 🔥 endpoint ligero
        .then(res => res.json())
        .then(data => {

            data.forEach(srv => {

                let card = document.getElementById("card-" + srv.name);
                if (!card) return;

                // ✅ eliminar clases previas
                card.classList.remove("card-up", "card-down", "card-warning", "down-alert");

                // ✅ aplicar estado nuevo
                if (!srv.up) {
                    card.classList.add("card-down", "down-alert");
                } else if (srv.responseTime > 1000) {
                    card.classList.add("card-warning");
                } else {
                    card.classList.add("card-up");
                }

                // ✅ actualizar tiempo
                let timeText = card.querySelector(".small-text:last-child");
                if (timeText) {
                    timeText.innerText = "Tiempo: " + srv.responseTime + " ms";
                }

                // ✅ actualizar icono
                let icon = card.querySelector("span");
                if (icon) {
                    if (!srv.up) icon.innerText = "🔴";
                    else if (srv.responseTime > 1000) icon.innerText = "🟡";
                    else icon.innerText = "🟢";
                }

                // ✅ actualizar estado texto
                let statusLabel = card.querySelector(".status-label");
                if (statusLabel) {
                    if (!srv.up) statusLabel.innerText = "DOWN";
                    else if (srv.responseTime > 1000) statusLabel.innerText = "LENTO";
                    else statusLabel.innerText = "UP";
                }

            });

        })
        .catch(err => console.error("Error cards:", err));
}


// ======================================================
// ✅ IMPACTO / DEPENDENCIAS
// ======================================================
let impactoLoaded = false;
let impactoRankingChart = null;
let impactoNetwork = null;
let impactoAllServicios = [];
let impactoDominios = [];

function criticidadColor(criticidad) {
    if (criticidad === 'Crítica') return '#dc3545';
    if (criticidad === 'Alta') return '#fd7e14';
    if (criticidad === 'Media') return '#ffc107';
    return '#28a745';
}

function criticidadHeatClass(criticidad) {
    if (criticidad === 'Crítica' || criticidad === 'Alta') return 'heat-alta';
    if (criticidad === 'Media') return 'heat-media';
    return 'heat-baja';
}

function loadImpactoDashboard() {

    fetch('/api/impacto/dashboard')
        .then(res => res.json())
        .then(data => {

            document.getElementById("kpiDominios").innerText = data.totalDominiosAnalizados;
            document.getElementById("kpiServicios").innerText = data.totalServiciosRestservices;
            document.getElementById("kpiDependencias").innerText = data.totalServiciosConCoincidencia;
            document.getElementById("kpiCriticos").innerText = (data.serviciosCriticos + data.serviciosAltos);
            document.getElementById("kpiSpof").innerText = data.spofDetectados;
            document.getElementById("impactoCobertura").innerText =
                "Cobertura por nombre: " + data.coberturaPct + "%";

            document.getElementById("impactoFallbackAlert").style.display =
                data.usandoDatosFallback ? "block" : "none";

            impactoDominios = data.dominios || [];

            let select = document.getElementById("impactoDomainSelect");
            select.innerHTML = '<option value="">Todos los dominios</option>';

            impactoDominios.forEach(dom => {
                let opt = document.createElement("option");
                opt.value = dom;
                opt.textContent = dom;
                select.appendChild(opt);
            });
        })
        .catch(err => console.error("Error cargando dashboard de impacto:", err));
}

function loadImpactoServicios() {

    fetch('/api/impacto/servicios')
        .then(res => res.json())
        .then(data => {
            impactoAllServicios = data;
            renderImpactoVistas(impactoAllServicios);
        })
        .catch(err => console.error("Error cargando servicios de impacto:", err));
}

function filterImpactoByDomain(dominio) {

    if (!dominio) {
        renderImpactoVistas(impactoAllServicios);
        return;
    }

    let filtrados = impactoAllServicios.filter(s => (s.domains || []).includes(dominio));
    renderImpactoVistas(filtrados);
}

function renderImpactoVistas(servicios) {
    renderImpactoRanking(servicios);
    renderImpactoMatrix(servicios);
    renderImpactoGraph(servicios);
}

function renderImpactoRanking(servicios) {

    let conImpacto = servicios.filter(s => s.cantidad > 0)
        .sort((a, b) => b.pct - a.pct);

    const ctx = document.getElementById("impactoRankingChart");

    if (impactoRankingChart) impactoRankingChart.destroy();

    if (conImpacto.length === 0) {
        return;
    }

    impactoRankingChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: conImpacto.map(s => s.name),
            datasets: [{
                label: '% Afectación',
                data: conImpacto.map(s => s.pct),
                backgroundColor: conImpacto.map(s => criticidadColor(s.criticidad))
            }]
        },
        options: {
            indexAxis: 'y',
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: { x: { min: 0, max: 100 } },
            onClick: (evt, elements) => {
                if (elements.length > 0) {
                    showImpactoDetail(conImpacto[elements[0].index].name);
                }
            }
        }
    });
}

function renderImpactoMatrix(servicios) {

    let conImpacto = servicios.filter(s => s.cantidad > 0)
        .sort((a, b) => b.pct - a.pct);

    let head = document.getElementById("impactoMatrixHead");
    let body = document.getElementById("impactoMatrixBody");

    let headerCols = '<th>Servicio</th>' +
        impactoDominios.map(d => '<th>' + d + '</th>').join('') +
        '<th>%</th><th>Criticidad</th>';

    head.innerHTML = '<tr>' + headerCols + '</tr>';

    if (conImpacto.length === 0) {
        body.innerHTML = '<tr><td colspan="' + (impactoDominios.length + 3) +
            '" class="text-center text-muted">Sin dependencias para este filtro</td></tr>';
        return;
    }

    body.innerHTML = conImpacto.map(s => {

        let cells = impactoDominios.map(dom => {
            let afecta = (s.domains || []).includes(dom);
            let cls = afecta ? criticidadHeatClass(s.criticidad) : '';
            return '<td class="' + cls + ' text-center">' + (afecta ? '●' : '') + '</td>';
        }).join('');

        return '<tr style="cursor:pointer" onclick="showImpactoDetail(\'' + s.name + '\')">' +
            '<td>' + s.name + '</td>' + cells +
            '<td class="text-center">' + s.pct + '%</td>' +
            '<td class="text-center">' +
            '<span class="badge" style="background-color:' + criticidadColor(s.criticidad) + '">' +
            s.criticidad + '</span></td></tr>';
    }).join('');
}

function renderImpactoGraph(servicios) {

    let container = document.getElementById("impactoGraph");

    if (impactoNetwork) {
        impactoNetwork.destroy();
        impactoNetwork = null;
    }

    let conImpacto = servicios.filter(s => s.cantidad > 0);

    let nodes = [{ id: 'osba', label: 'osba', shape: 'box', color: '#0b4ea2', font: { color: '#fff' } }];
    let edges = [];
    let dominiosEnGrafo = new Set();

    conImpacto.forEach(s => {
        (s.domains || []).forEach(dom => dominiosEnGrafo.add(dom));
    });

    dominiosEnGrafo.forEach(dom => {
        nodes.push({ id: 'dom-' + dom, label: dom, shape: 'ellipse', color: '#6366f1', font: { color: '#fff' } });
        edges.push({ from: 'osba', to: 'dom-' + dom });
    });

    conImpacto.forEach(s => {

        nodes.push({
            id: 'srv-' + s.name,
            label: s.name,
            shape: 'box',
            color: criticidadColor(s.criticidad),
            font: { color: '#fff', size: 11 }
        });

        (s.domains || []).forEach(dom => {
            edges.push({ from: 'srv-' + s.name, to: 'dom-' + dom });
        });
    });

    impactoNetwork = new vis.Network(container, { nodes: nodes, edges: edges }, {
        physics: { stabilization: true },
        interaction: { hover: true, navigationButtons: false, zoomView: true, dragView: true },
        nodes: { margin: 8 }
    });

    impactoNetwork.once("stabilized", function () {
        impactoNetwork.fit({ animation: true });
    });

    impactoNetwork.on('click', params => {

        if (params.nodes.length === 0) return;

        let id = params.nodes[0];

        if (id.startsWith('srv-')) {
            showImpactoDetail(id.substring('srv-'.length));
        } else if (id.startsWith('dom-')) {
            let dom = id.substring('dom-'.length);
            document.getElementById("impactoDomainSelect").value = dom;
            filterImpactoByDomain(dom);
        }
    });
}

function showImpactoDetail(name) {

    fetch('/api/impacto/ficha/' + encodeURIComponent(name))
        .then(res => {
            if (!res.ok) {
                throw new Error('Ficha no encontrada');
            }
            return res.json();
        })
        .then(ficha => {

            // ============================
            // CABECERA
            // ============================
            document.getElementById("impactoModalName").innerText =
                ficha.servicio || "-";

            // ============================
            // INFORMACION GENERAL
            // ============================
            document.getElementById("fichaServicio").innerText =
                ficha.servicio || "-";

            document.getElementById("fichaVersion").innerText =
                ficha.version || "-";

            document.getElementById("fichaDominio").innerText =
                ficha.dominio || "-";

            document.getElementById("fichaEndpoints").innerText =
                ficha.endpoints || 0;

            document.getElementById("fichaConsumidores").innerText =
                ficha.consumidores || 0;

            document.getElementById("fichaDependencias").innerText =
                ficha.dependencias || 0;

            document.getElementById("fichaImpacto").innerText =
                (ficha.impactoPct || 0) + "%";

            document.getElementById("fichaCobertura").innerText =
                ficha.cobertura || "-";

            // ============================
            // CRITICIDAD
            // ============================
            let badge = document.getElementById("fichaCriticidad");

            badge.innerText = ficha.criticidad || "-";
            badge.style.backgroundColor =
                criticidadColor(ficha.criticidad);

            badge.style.color = "#fff";

            // ============================
            // INFORMACION TECNICA
            // ============================
            document.getElementById("fichaJava").innerText =
                ficha.javaVersion || "-";

            document.getElementById("fichaSpring").innerText =
                ficha.springBootVersion || "-";

            document.getElementById("fichaArtifact").innerText =
                ficha.artifactId || "-";

            document.getElementById("fichaPackaging").innerText =
                ficha.packaging || "-";

            // ============================
            // SWAGGER
            // ============================
            document.getElementById("fichaSwaggerUrl").innerText =
                ficha.swaggerUrl || "-";

            // ============================
            // DEPENDENCIAS
            // ============================
            let deps =
                document.getElementById("fichaDependenciasList");

            deps.innerHTML = "";

            if (!ficha.consume || ficha.consume.length === 0) {

                deps.innerHTML =
                    '<li class="list-group-item text-muted">Sin dependencias registradas</li>';

            } else {

                ficha.consume.forEach(dep => {

                    deps.innerHTML += `
                        <li class="list-group-item">
                            ${dep}
                        </li>
                    `;
                });
            }

            // ============================
            // IMPACTADOS
            // ============================
            let impactados =
                document.getElementById("fichaImpactadosList");

            impactados.innerHTML = "";

            if (!ficha.impactados ||
                ficha.impactados.length === 0) {

                impactados.innerHTML =
                    '<li class="list-group-item text-muted">Sin dominios impactados</li>';

            } else {

                ficha.impactados.forEach(dom => {

                    impactados.innerHTML += `
                        <li class="list-group-item">
                            ${dom}
                        </li>
                    `;
                });
            }

            // ============================
            // MOSTRAR MODAL
            // ============================
            new bootstrap.Modal(
                document.getElementById('impactoModal')
            ).show();

        })
        .catch(err => {
            console.error(
                "Error cargando ficha técnica:",
                err
            );
        });
}

// ======================================================
// ✅ MODAL LISTA GENERICA (KPIs)
// ======================================================
function showImpactoList(titulo, items, onItemClick) {

    document.getElementById("impactoListModalTitle").innerText = titulo;

    let body = document.getElementById("impactoListModalBody");
    body.innerHTML = "";

    if (!items || items.length === 0) {
        body.innerHTML = '<li class="list-group-item text-muted">Sin elementos</li>';
    } else {

        items.forEach(function (item) {

            let li = document.createElement("li");
            li.className = "list-group-item d-flex justify-content-between align-items-center";
            li.style.cursor = "pointer";
            li.innerText = item.label;

            if (item.badge) {
                let badge = document.createElement("span");
                badge.className = "badge";
                badge.style.backgroundColor = item.badgeColor || "#6c757d";
                badge.innerText = item.badge;
                li.appendChild(badge);
            }

            li.addEventListener("click", function () {
                bootstrap.Modal.getInstance(document.getElementById('impactoListModal')).hide();
                if (onItemClick) onItemClick(item);
            });

            body.appendChild(li);
        });
    }

    new bootstrap.Modal(document.getElementById('impactoListModal')).show();
}

function kpiDominiosClick() {

    let items = impactoDominios.map(d => ({ label: d }));

    showImpactoList("Dominios analizados (" + impactoDominios.length + ")", items, function (item) {
        document.getElementById("impactoDomainSelect").value = item.label;
        filterImpactoByDomain(item.label);
    });
}

function kpiServiciosClick() {

    let items = impactoAllServicios
        .slice()
        .sort((a, b) => a.name.localeCompare(b.name))
        .map(s => ({
            label: s.name,
            badge: s.cantidad > 0 ? s.criticidad : null,
            badgeColor: criticidadColor(s.criticidad)
        }));

    showImpactoList("Servicios totales (" + items.length + ")", items, function (item) {
        showImpactoDetail(item.label);
    });
}

function kpiDependenciasClick() {

    let items = impactoAllServicios
        .filter(s => s.cantidad > 0)
        .sort((a, b) => b.pct - a.pct)
        .map(s => ({ label: s.name, badge: s.criticidad, badgeColor: criticidadColor(s.criticidad) }));

    showImpactoList("Dependencias identificadas (" + items.length + ")", items, function (item) {
        showImpactoDetail(item.label);
    });
}

function kpiCriticosClick() {

    let items = impactoAllServicios
        .filter(s => s.criticidad === 'Alta' || s.criticidad === 'Crítica')
        .sort((a, b) => b.pct - a.pct)
        .map(s => ({ label: s.name, badge: s.criticidad, badgeColor: criticidadColor(s.criticidad) }));

    showImpactoList("Servicios críticos / altos (" + items.length + ")", items, function (item) {
        showImpactoDetail(item.label);
    });
}

document.addEventListener("DOMContentLoaded", function () {

    document.getElementById("tab-impacto-btn").addEventListener("shown.bs.tab", function () {
        if (!impactoLoaded) {
            impactoLoaded = true;
            loadImpactoDashboard();
            loadImpactoServicios();
        }
    });

    document.getElementById("impactoDomainSelect").addEventListener("change", function () {
        filterImpactoByDomain(this.value);
    });
});
