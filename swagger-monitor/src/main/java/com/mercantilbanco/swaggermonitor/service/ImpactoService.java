package com.mercantilbanco.swaggermonitor.service;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercantilbanco.swaggermonitor.model.DomainImpact;
import com.mercantilbanco.swaggermonitor.model.ImpactoDashboard;
import com.mercantilbanco.swaggermonitor.model.ServiceImpact;
import org.springframework.beans.factory.annotation.Value;

/**
 * CLASE ENCARGADA DE: <strong>analizar el impacto/dependencias del
microservicio osba (restservices.xml)
 * contra los dominios de negocio (services.xml) bajo %BM_HOME%\appl</strong>.
 *
 * Supuestos de negocio (no especificados explicitamente en la propuesta):
 * <ul>
 *   <li>Si BM_HOME no está seteado, o la carpeta
appl/osba/config/restservices.xml no existe, se usa como
 *       respaldo un snapshot embebido en el classpath ({@code
impacto-fallback.json}, copia del análisis ya
 *       entregado en docs/impacto-dependencias/afectacion.json),
igual que ServerMonitorService cae a su
 *       servers.xml interno cuando no encuentra el archivo externo.</li>
 *   <li>Si existiera más de un restservices.xml bajo appl/*, solo se
usa el del microservicio objetivo
 *       ("osba"), ignorando cualquier otro.</li>
 *   <li>El nombre de dominio se deriva del nombre de carpeta bajo
appl/ quitando el prefijo "api-" y
 *       capitalizando la primera letra (p. ej. "api-persona" -&gt;
"Persona"); si la carpeta no sigue ese
 *       patrón se usa el nombre de carpeta tal cual.</li>
 *   <li>La carpeta del propio microservicio objetivo ("osba") se
excluye de la lista de dominios: su
 *       services.xml es su integración legacy SOAP/CICS, no un
dominio de negocio.</li>
 *   <li>Carpetas adicionales a excluir del escaneo de dominios (p. ej.
"swagger-generator") se configuran
 *       vía la propiedad {@code impacto.dominios-excluidos} en
application.yaml (lista separada por comas),
 *       no están hardcodeadas en el código.</li>
 *   <li>Escala de criticidad por % de afectación: 0% Baja, 1-20%
Baja, 21-40% Media, 41-60% Alta, 61-100%
 *       Crítica (igual a las bandas descritas en el informe de impacto).</li>
 *   <li>Un servicio se considera SPOF potencial cuando depende de
exactamente un dominio (cantidad == 1) y
 *       ese dominio es el único canal que lo resuelve.</li>
 *   <li>El cruce ya NO es solo por {@code name}: un servicio de
restservices.xml también se considera
 *       dependiente de un dominio si el último segmento de su
{@code endpoint} (el identificador de
 *       programa/transacción backend, p. ej. "...-gipw106x") coincide
con el de algún servicio de ese
 *       dominio, igual que la evidencia secundaria descrita en la
sección 7 del informe de impacto. Para
 *       evitar falsos positivos con segmentos genéricos (p. ej.
"payment", "companies"), solo se usa como
 *       evidencia un segmento de al menos {@value #SUFIJO_MIN_LENGTH}
caracteres.</li>
 * </ul>
 *
 * @author <strong>JOSE GUERRA</strong>
 * <hr><code>Copyright 02/11/2026. Banco Mercantil, All rights reserved.</code>
 */
@Service
public class ImpactoService {

    private static final Logger log =
LogManager.getLogger(ImpactoService.class);

    private static final String MICROSERVICIO = "osba";
    private static final String APPL_DIR = "appl";
    private static final String CONFIG_DIR = "config";
    private static final String SERVICES_FILE = "services.xml";
    private static final String RESTSERVICES_FILE = "restservices.xml";
    private static final String DOMAIN_PREFIX = "api-";
    private static final String TAG_SERVICE = "service";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_ENDPOINT = "endpoint";
    private static final String FALLBACK_RESOURCE = "impacto-fallback.json";
    private static final int SUFIJO_MIN_LENGTH = 12;

    @Value("${impacto.dominios-excluidos:}")
    private List<String> dominiosExcluidos;

    @Cacheable("impactoData")
    public ImpactoResult analizar() {

        String bmHome = resolveBmHome();

        if (bmHome != null) {

            File applDir = new File(bmHome, APPL_DIR);
            File restservicesFile = new File(new File(new
File(applDir, MICROSERVICIO), CONFIG_DIR), RESTSERVICES_FILE);

            if (applDir.isDirectory() && restservicesFile.isFile()) {

                try {
                    return analizarDesdeFilesystem(applDir, restservicesFile);
                } catch (Exception e) {
                    log.error("❌ Error analizando impacto/dependencias desde BM_HOME, usando datos de respaldo", e);
                }
            } else {
                log.warn("⚠️ BM_HOME definido pero no se encontró appl/{}/config/{} ; usando datos de respaldo",
                        MICROSERVICIO, RESTSERVICES_FILE);
            }
        } else {
            log.warn("⚠️ BM_HOME no está configurado; usando datos de respaldo embebidos ({})", FALLBACK_RESOURCE);
        }

        return analizarDesdeFallback();
    }

    private boolean dominioExcluido(String folderName) {

        if (dominiosExcluidos == null) {
            return false;
        }

        return dominiosExcluidos.stream()
                .anyMatch(d -> d.equalsIgnoreCase(folderName));
    }

    private String resolveBmHome() {

        String bmHome = System.getenv("BM_HOME");

        if (bmHome == null || bmHome.trim().isEmpty()) {
            bmHome = System.getProperty("BM_HOME");
        }

        return (bmHome == null || bmHome.trim().isEmpty()) ? null :
bmHome.trim();
    }

    // =========================================================
    // ✅ ANÁLISIS DESDE FILESYSTEM (BM_HOME real)
    // =========================================================
    private ImpactoResult analizarDesdeFilesystem(File applDir, File
restservicesFile) throws Exception {

        Map<String, DomainIndex> dominioIndex = new LinkedHashMap<>();

        File[] carpetas = applDir.listFiles(File::isDirectory);

        if (carpetas != null) {
            for (File carpeta : carpetas) {

                if (MICROSERVICIO.equalsIgnoreCase(carpeta.getName())) {
                    continue;
                }

                if (dominioExcluido(carpeta.getName())) {
                    continue;
                }

                File servicesXml = new File(new File(carpeta,
CONFIG_DIR), SERVICES_FILE);

                if (!servicesXml.isFile()) {
                    continue;
                }

                DomainIndex index = construirIndiceDominio(servicesXml);

                dominioIndex.put(toDomainName(carpeta.getName()), index);
            }
        }

        List<ServiceImpact> servicios =
cruzarRestservices(restservicesFile, dominioIndex);

        return new ImpactoResult(servicios, dominioIndex, false);
    }

    private String toDomainName(String folderName) {

        String lower = folderName.toLowerCase();

        if (lower.startsWith(DOMAIN_PREFIX) && lower.length() >
DOMAIN_PREFIX.length()) {
            String resto = folderName.substring(DOMAIN_PREFIX.length());
            return capitalize(resto);
        }

        return capitalize(folderName);
    }

    private String capitalize(String value) {

        if (value == null || value.isEmpty()) {
            return value;
        }

        return value.substring(0, 1).toUpperCase() +
value.substring(1).toLowerCase();
    }

    private List<ServiceImpact> cruzarRestservices(File
restservicesFile, Map<String, DomainIndex> dominios)
            throws Exception {

        Document doc = parseXml(restservicesFile);
        NodeList nodes = doc.getElementsByTagName(TAG_SERVICE);

        List<ServiceImpact> servicios = new ArrayList<>();

        for (int i = 0; i < nodes.getLength(); i++) {

            Element el = (Element) nodes.item(i);
            String name = el.getAttribute(ATTR_NAME);

            if (name == null || name.trim().isEmpty()) {
                continue;
            }

            String nombre = name.trim();
            String endpoint = el.getAttribute(ATTR_ENDPOINT);
            String sufijo = extraerSufijoEndpoint(endpoint);

            ServiceImpact si = new ServiceImpact();
            si.setName(nombre);
            si.setEndpoint(endpoint);

            for (Map.Entry<String, DomainIndex> dominio : dominios.entrySet()) {

                DomainIndex indice = dominio.getValue();
                boolean coincideNombre = indice.getNombres().contains(nombre);
                boolean coincideSufijo = sufijo != null &&
indice.getSufijos().contains(sufijo);

                if (coincideNombre || coincideSufijo) {
                    si.getDomains().add(dominio.getKey());
                }
            }

            aplicarCalculos(si, dominios.size());
            servicios.add(si);
        }

        return servicios;
    }

    private DomainIndex construirIndiceDominio(File xmlFile) throws Exception {

        Document doc = parseXml(xmlFile);
        NodeList nodes = doc.getElementsByTagName(TAG_SERVICE);

        DomainIndex index = new DomainIndex();

        for (int i = 0; i < nodes.getLength(); i++) {

            Element el = (Element) nodes.item(i);
            String name = el.getAttribute(ATTR_NAME);
            String endpoint = el.getAttribute(ATTR_ENDPOINT);

            if (name != null && !name.trim().isEmpty()) {
                index.getNombres().add(name.trim());
                index.incrementarTotalServicios();
            }

            String sufijo = extraerSufijoEndpoint(endpoint);

            if (sufijo != null) {
                index.getSufijos().add(sufijo);
            }
        }

        return index;
    }

    /**
     * Extrae el último segmento de la ruta del endpoint (el
     * identificador de programa/transacción backend, p. ej.
     * "consultarTelefonosDetallado-gipw106x"), en minúsculas para
     * comparación case-insensitive. Devuelve null si el endpoint es
     * vacío o si el segmento es demasiado corto/genérico (menos de
     * {@value #SUFIJO_MIN_LENGTH} caracteres) para servir de evidencia
     * confiable, igual que la advertencia de falsos positivos del
     * informe de impacto (p. ej. "payment", "companies").
     */
    private String extraerSufijoEndpoint(String endpoint) {

        if (endpoint == null || endpoint.trim().isEmpty()) {
            return null;
        }

        String limpio = endpoint.trim();

        int queryIdx = limpio.indexOf('?');
        if (queryIdx >= 0) {
            limpio = limpio.substring(0, queryIdx);
        }

        while (limpio.endsWith("/")) {
            limpio = limpio.substring(0, limpio.length() - 1);
        }

        int slashIdx = limpio.lastIndexOf('/');
        String segmento = (slashIdx >= 0) ? limpio.substring(slashIdx + 1) : limpio;

        if (segmento.length() < SUFIJO_MIN_LENGTH) {
            return null;
        }

        return segmento.toLowerCase();
    }

    private Document parseXml(File file) throws Exception {

        try (InputStream is =
java.nio.file.Files.newInputStream(file.toPath())) {
            return
DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
        }
    }

    // =========================================================
    // ✅ RESPALDO (BM_HOME ausente o archivos no encontrados)
    // =========================================================
    private ImpactoResult analizarDesdeFallback() {

        try (InputStream is =
getClass().getClassLoader().getResourceAsStream(FALLBACK_RESOURCE)) {

            if (is == null) {
                throw new IllegalStateException("No se encontró " +
FALLBACK_RESOURCE + " en el classpath");
            }

            String json = new String(readAll(is), StandardCharsets.UTF_8);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            Map<String, DomainIndex> dominios = new LinkedHashMap<>();

            for (JsonNode dominioNode : root.path("dominios")) {
                dominios.put(dominioNode.asText(), new DomainIndex());
            }

            List<ServiceImpact> servicios = new ArrayList<>();

            for (JsonNode node : root.path("servicios")) {

                ServiceImpact si = new ServiceImpact();
                si.setName(node.path(ATTR_NAME).asText());
                si.setEndpoint(node.path(ATTR_ENDPOINT).asText(""));

                for (JsonNode dominioNode : node.path("domains")) {
                    si.getDomains().add(dominioNode.asText());
                }

                aplicarCalculos(si, dominios.size());
                servicios.add(si);
            }

            return new ImpactoResult(servicios, dominios, true);

        } catch (Exception e) {
            log.error("❌ No se pudo cargar el respaldo embebido de impacto/dependencias", e);
            return new ImpactoResult(new ArrayList<>(), new
LinkedHashMap<>(), true);
        }
    }

    private byte[] readAll(InputStream is) throws Exception {

        java.io.ByteArrayOutputStream buffer = new
java.io.ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int read;

        while ((read = is.read(data)) != -1) {
            buffer.write(data, 0, read);
        }

        return buffer.toByteArray();
    }

    private void aplicarCalculos(ServiceImpact si, int totalDominios) {

        int cantidad = si.getDomains().size();
        int pct = (totalDominios == 0) ? 0 : (int)
Math.round((cantidad * 100.0) / totalDominios);

        si.setCantidad(cantidad);
        si.setPct(pct);
        si.setCriticidad(criticidadPorPct(pct));
    }

    private String criticidadPorPct(int pct) {

        if (pct > 60) {
            return "Crítica";
        }
        if (pct > 40) {
            return "Alta";
        }
        if (pct > 20) {
            return "Media";
        }
        return "Baja";
    }

    // =========================================================
    // ✅ VISTAS PARA EL CONTROLLER
    // =========================================================
    public ImpactoDashboard getDashboard() {

        ImpactoResult result = analizar();
        List<ServiceImpact> servicios = result.getServicios();

        ImpactoDashboard dashboard = new ImpactoDashboard();
        dashboard.setMicroservicio(MICROSERVICIO);
        dashboard.setDominios(new ArrayList<>(result.getDominios().keySet()));
        dashboard.setTotalDominiosAnalizados(result.getDominios().size());
        dashboard.setTotalServiciosRestservices(servicios.size());
        dashboard.setUsandoDatosFallback(result.isFallback());

        long conCoincidencia = servicios.stream().filter(s ->
s.getCantidad() > 0).count();
        long criticos = servicios.stream().filter(s ->
"Crítica".equals(s.getCriticidad())).count();
        long altos = servicios.stream().filter(s ->
"Alta".equals(s.getCriticidad())).count();
        long spof = servicios.stream().filter(s -> s.getCantidad() ==
1).count();

        dashboard.setTotalServiciosConCoincidencia((int) conCoincidencia);
        dashboard.setCoberturaPct(servicios.isEmpty() ? 0 :
Math.round((conCoincidencia * 1000.0) / servicios.size()) / 10.0);
        dashboard.setServiciosCriticos((int) criticos);
        dashboard.setServiciosAltos((int) altos);
        dashboard.setSpofDetectados((int) spof);

        return dashboard;
    }

    public List<ServiceImpact> getRanking() {

        List<ServiceImpact> servicios = new
ArrayList<>(analizar().getServicios());
        servicios.sort(Comparator.comparingInt(ServiceImpact::getPct).reversed()
                .thenComparing(ServiceImpact::getName));

        return servicios;
    }

    public ServiceImpact getServicio(String name) {

        return analizar().getServicios().stream()
                .filter(s -> s.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public DomainImpact getDominio(String nombre) {

        ImpactoResult result = analizar();

        Map.Entry<String, DomainIndex> dominioEntry =
result.getDominios().entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(nombre))
                .findFirst()
                .orElse(null);

        if (dominioEntry == null) {
            return null;
        }

        DomainImpact di = new DomainImpact();
        di.setName(dominioEntry.getKey());
        di.setTotalServiciosDominio(dominioEntry.getValue().getTotalServicios());

        for (ServiceImpact si : result.getServicios()) {
            if (si.getDomains().stream().anyMatch(d ->
d.equalsIgnoreCase(nombre))) {
                di.getServiciosDependientes().add(si);
            }
        }

        return di;
    }

    // =========================================================
    // ✅ RESULTADO INTERNO CACHEADO
    // =========================================================
    public static final class ImpactoResult {

        private final List<ServiceImpact> servicios;
        private final Map<String, DomainIndex> dominios;
        private final boolean fallback;

        public ImpactoResult(List<ServiceImpact> servicios,
Map<String, DomainIndex> dominios, boolean fallback) {
            this.servicios = servicios;
            this.dominios = dominios;
            this.fallback = fallback;
        }

        public List<ServiceImpact> getServicios() {
            return servicios;
        }

        public Map<String, DomainIndex> getDominios() {
            return dominios;
        }

        public boolean isFallback() {
            return fallback;
        }
    }

    /**
     * Índice de un dominio de negocio: nombres de servicio y sufijos de
     * endpoint (ver {@link #extraerSufijoEndpoint(String)}) usados para
     * el cruce con restservices.xml, más el total de servicios propios
     * del dominio (para la vista "por dominio").
     */
    private static final class DomainIndex {

        private final Set<String> nombres = new LinkedHashSet<>();
        private final Set<String> sufijos = new LinkedHashSet<>();
        private int totalServicios;

        private Set<String> getNombres() {
            return nombres;
        }

        private Set<String> getSufijos() {
            return sufijos;
        }

        private int getTotalServicios() {
            return totalServicios;
        }

        private void incrementarTotalServicios() {
            totalServicios++;
        }
    }
}