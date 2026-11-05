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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercantilbanco.swaggermonitor.model.DomainImpact;
import com.mercantilbanco.swaggermonitor.model.ImpactoDashboard;
import com.mercantilbanco.swaggermonitor.model.ServiceFichaDTO;
import com.mercantilbanco.swaggermonitor.model.ServiceImpact;


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

    private static final Logger log = LogManager.getLogger(ImpactoService.class);

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

    // Servicios o directorios en el BM_HOME que deben ser excluidos
    @Value("#{'${impacto.dominios-excluidos:}'.split(',')}")
    private List<String> dominiosExcluidos;

    @Autowired
    private Environment environment;

    //@Cacheable("impactoData")
    public ImpactoResult analizar() {

        String bmHome = resolveBmHome();

        if (bmHome != null) {

            File applDir = new File(bmHome, APPL_DIR);
            File restservicesFile = new File(new File(new File(applDir, MICROSERVICIO), CONFIG_DIR), RESTSERVICES_FILE);

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

        if (dominiosExcluidos == null || dominiosExcluidos.isEmpty()) {
            return false;
        }

        String normalized = folderName
                .replaceFirst("^api-", "")
                .trim()
                .toLowerCase();

        return dominiosExcluidos.stream()
                .filter(d -> d != null)
                .map(String::trim)
                .map(String::toLowerCase)
                .anyMatch(normalized::equals);
    }

    private String resolveBmHome() {

        String bmHome = System.getenv("BM_HOME");

        if (bmHome == null || bmHome.trim().isEmpty()) {
            bmHome = System.getProperty("BM_HOME");
        }

        return (bmHome == null || bmHome.trim().isEmpty()) ? null : bmHome.trim();
    }

    // =========================================================
    // ✅ ANÁLISIS DESDE FILESYSTEM (BM_HOME real)
    // =========================================================
    private ImpactoResult analizarDesdeFilesystem(File applDir, File restservicesFile) throws Exception {

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

                File servicesXml = new File(new File(carpeta, CONFIG_DIR), SERVICES_FILE);

                if (!servicesXml.isFile()) {
                    continue;
                }

                DomainIndex index = construirIndiceDominio(servicesXml);

                dominioIndex.put(toDomainName(carpeta.getName()), index);
            }
        }

        List<ServiceImpact> servicios = cruzarRestservices(restservicesFile, dominioIndex);

        return new ImpactoResult(servicios, dominioIndex, false);
    }

    private String toDomainName(String folderName) {

        String lower = folderName.toLowerCase();

        if (lower.startsWith(DOMAIN_PREFIX) && lower.length() > DOMAIN_PREFIX.length()) {
            String resto = folderName.substring(DOMAIN_PREFIX.length());
            return capitalize(resto);
        }

        return capitalize(folderName);
    }

    private String capitalize(String value) {

        if (value == null || value.isEmpty()) {
            return value;
        }

        return value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
    }

    private List<ServiceImpact> cruzarRestservices(File restservicesFile, Map<String, DomainIndex> dominios) throws Exception {

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
                boolean coincideSufijo = sufijo != null && indice.getSufijos().contains(sufijo);

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

        try (InputStream is = java.nio.file.Files.newInputStream(file.toPath())) {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
        }
    }

    // =========================================================
    // ✅ RESPALDO (BM_HOME ausente o archivos no encontrados)
    // =========================================================
    private ImpactoResult analizarDesdeFallback() {

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(FALLBACK_RESOURCE)) {

            if (is == null) {
                throw new IllegalStateException("No se encontró " + FALLBACK_RESOURCE + " en el classpath");
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
            return new ImpactoResult(new ArrayList<>(), new LinkedHashMap<>(), true);
        }
    }

    private byte[] readAll(InputStream is) throws Exception {

        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int read;

        while ((read = is.read(data)) != -1) {
            buffer.write(data, 0, read);
        }

        return buffer.toByteArray();
    }

    private void aplicarCalculos(ServiceImpact si, int totalDominios) {

        int cantidad = si.getDomains().size();
        int pct = (totalDominios == 0) ? 0 : (int) Math.round((cantidad * 100.0) / totalDominios);

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

        long conCoincidencia = servicios.stream().filter(s -> s.getCantidad() > 0).count();
        long criticos = servicios.stream().filter(s -> "Crítica".equals(s.getCriticidad())).count();
        long altos = servicios.stream().filter(s -> "Alta".equals(s.getCriticidad())).count();
        long spof = servicios.stream().filter(s -> s.getCantidad() == 1).count();

        dashboard.setTotalServiciosConCoincidencia((int) conCoincidencia);
        dashboard.setCoberturaPct(servicios.isEmpty() ? 0 : Math.round((conCoincidencia * 1000.0) / servicios.size()) / 10.0);
        dashboard.setServiciosCriticos((int) criticos);
        dashboard.setServiciosAltos((int) altos);
        dashboard.setSpofDetectados((int) spof);

        return dashboard;
    }

    public List<ServiceImpact> getRanking() {

        List<ServiceImpact> servicios = new ArrayList<>(analizar().getServicios());
        servicios.sort(Comparator.comparingInt(ServiceImpact::getPct).reversed().thenComparing(ServiceImpact::getName));

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

        Map.Entry<String, DomainIndex> dominioEntry = result.getDominios().entrySet().stream()
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
            if (si.getDomains().stream().anyMatch(d -> d.equalsIgnoreCase(nombre))) {
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

        public ImpactoResult(List<ServiceImpact> servicios, Map<String, DomainIndex> dominios, boolean fallback) {
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
    
    public ServiceFichaDTO getFichaTecnica(String nombre) {

        ServiceImpact servicio = getServicio(nombre);

        if (servicio == null) {
            return null;
        }

        ServiceFichaDTO ficha = new ServiceFichaDTO();

        ficha.setServicio(servicio.getName());

        ficha.setImpactoPct(servicio.getPct());

        ficha.setCriticidad(servicio.getCriticidad());

        ficha.setDependencias(servicio.getCantidad());

        ficha.setCobertura(
                servicio.getCantidad() > 0 ? "100%" : "0%");

        ficha.setVersion("N/D");
        ficha.setDominio(
        	    servicio.getDomains().isEmpty()
        	        ? "N/D"
        	        : String.join(", ", servicio.getDomains())
        	);

        ficha.setEndpoints(0);
    	ficha.setConsumidores(
        	    servicio.getDomains().size()
        	);

        String dominioPrincipal = servicio.getDomains().isEmpty() ? null : servicio.getDomains().get(0);
        completarDatosTecnicos(ficha, dominioPrincipal);

        ficha.setSwaggerUrl(
                "/swagger/" +
                servicio.getName().toUpperCase() +
                ".json");

        ficha.setImpactados(
                new ArrayList<>(servicio.getDomains()));

        List<String> consume = new ArrayList<>();

        if (servicio.getEndpoint() != null &&
            !servicio.getEndpoint().trim().isEmpty()) {

            consume.add(servicio.getEndpoint());
        }

        ficha.setConsume(consume);

        return ficha;
    }

    /**
     * Completa javaVersion/springBootVersion/artifactId/packaging (y
     * version, si se encuentra) leyendo el artefacto real del
     * microservicio dueño del dominio, cuya ruta se configura en
     * application.yaml bajo {@code impacto.pom-paths.<Dominio>} (ver
     * ejemplo comentado en el archivo). La ruta puede apuntar a un
     * {@code pom.xml} suelto (si hay acceso a fuentes) o, el caso más
     * común en un servidor sin fuentes, directamente a un {@code .jar}
     * o {@code .war} ya compilado: en ese caso se lee el {@code pom.xml}
     * que Maven embebe por defecto en
     * {@code META-INF/maven/<groupId>/<artifactId>/pom.xml} dentro del
     * propio archivo, y además {@code META-INF/MANIFEST.MF} (de donde
     * se toma {@code Spring-Boot-Version} si el jar es un ejecutable de
     * Spring Boot, más confiable que inferirla del {@code <parent>}).
     * Si el dominio es null, no hay ruta configurada para él, o el
     * archivo no se puede leer, deja "N/D" en los 4 campos sin lanzar
     * error.
     */
    private void completarDatosTecnicos(ServiceFichaDTO ficha, String dominio) {

        ficha.setJavaVersion("N/D");
        ficha.setSpringBootVersion("N/D");
        ficha.setArtifactId("N/D");
        ficha.setPackaging("N/D");

        if (dominio == null) {
            return;
        }

        String rutaConfigurada = environment.getProperty("impacto.pom-paths." + dominio);

        if (rutaConfigurada == null || rutaConfigurada.trim().isEmpty()) {
            return;
        }

        File archivo = new File(rutaConfigurada.trim());

        if (!archivo.isFile()) {
            log.warn("⚠️ No se encontró el pom.xml/jar/war configurado para el dominio {} en {}", dominio, rutaConfigurada);
            return;
        }

        try {

            String nombreArchivo = archivo.getName().toLowerCase();
            boolean esArchivoComprimido = nombreArchivo.endsWith(".jar") || nombreArchivo.endsWith(".war");

            PomInfo info = esArchivoComprimido ? leerPomDesdeArchivoComprimido(archivo) : leerPom(archivo);

            if (info.artifactId != null) {
                ficha.setArtifactId(info.artifactId);
            }
            if (info.packaging != null) {
                ficha.setPackaging(info.packaging);
            }
            if (info.javaVersion != null) {
                ficha.setJavaVersion(info.javaVersion);
            }
            if (info.springBootVersion != null) {
                ficha.setSpringBootVersion(info.springBootVersion);
            }
            if (info.version != null) {
                ficha.setVersion(info.version);
            }

        } catch (Exception e) {
            log.error("❌ Error leyendo datos tecnicos del dominio {} ({})", dominio, rutaConfigurada, e);
        }
    }

    private PomInfo leerPom(File pomFile) throws Exception {

        Document doc = parseXml(pomFile);
        return extraerPomInfo(doc.getDocumentElement());
    }

    private static final java.util.regex.Pattern SPRING_BOOT_LIB_PATTERN =
            java.util.regex.Pattern.compile(".*/spring-boot-(\\d+\\.\\d+\\.\\d+(?:[.-][A-Za-z0-9]+)?)\\.jar$");

    /**
     * Busca dentro del jar/war la entrada
     * {@code META-INF/maven/<groupId>/<artifactId>/pom.xml} que Maven
     * embebe por defecto (sin recorrer los jars de dependencias
     * anidados en BOOT-INF/lib o WEB-INF/lib, que son archivos aparte,
     * no entradas de este zip), y complementa/corrige el resultado con
     * {@code META-INF/MANIFEST.MF} cuando está disponible.
     *
     * Si aun asi falta javaVersion o springBootVersion (pom sin
     * {@code <properties><java.version>}, o con un parent/BOM propio
     * del banco en vez de {@code spring-boot-starter-parent}), se
     * toman del artefacto real en vez de quedar en "N/D":
     * <ul>
     *   <li>javaVersion: del "major version" del bytecode de cualquier
     *       {@code .class} dentro del archivo (el JDK real con el que
     *       se compilo, que es con el que realmente corre).</li>
     *   <li>springBootVersion: si es un jar/war empaquetado con
     *       Spring Boot, el propio {@code spring-boot-<version>.jar}
     *       viene embebido en {@code BOOT-INF/lib/} o
     *       {@code WEB-INF/lib/}; se toma la version de ese nombre de
     *       archivo.</li>
     * </ul>
     */
    private PomInfo leerPomDesdeArchivoComprimido(File archivo) throws Exception {

        PomInfo info = null;
        String springBootVersionManifest = null;
        String implementationVersionManifest = null;
        String springBootVersionLib = null;
        Integer claseMajorVersion = null;

        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(archivo)) {

            java.util.zip.ZipEntry pomEntry = null;
            java.util.zip.ZipEntry manifestEntry = null;
            java.util.zip.ZipEntry claseEntry = null;

            java.util.Enumeration<? extends java.util.zip.ZipEntry> entradas = zip.entries();

            while (entradas.hasMoreElements()) {

                java.util.zip.ZipEntry entrada = entradas.nextElement();
                String nombre = entrada.getName();

                if (pomEntry == null && nombre.startsWith("META-INF/maven/") && nombre.endsWith("/pom.xml")) {
                    pomEntry = entrada;
                }

                if ("META-INF/MANIFEST.MF".equals(nombre)) {
                    manifestEntry = entrada;
                }

                if (claseEntry == null && !entrada.isDirectory() && nombre.endsWith(".class")
                        && !nombre.endsWith("module-info.class")) {
                    claseEntry = entrada;
                }

                if (springBootVersionLib == null) {

                    java.util.regex.Matcher m = SPRING_BOOT_LIB_PATTERN.matcher(nombre);

                    if (m.matches()) {
                        springBootVersionLib = m.group(1);
                    }
                }
            }

            if (pomEntry != null) {
                try (InputStream is = zip.getInputStream(pomEntry)) {
                    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
                    info = extraerPomInfo(doc.getDocumentElement());
                }
            }

            if (manifestEntry != null) {
                try (InputStream is = zip.getInputStream(manifestEntry)) {

                    java.util.jar.Manifest manifest = new java.util.jar.Manifest(is);
                    java.util.jar.Attributes attrs = manifest.getMainAttributes();

                    springBootVersionManifest = attrs.getValue("Spring-Boot-Version");
                    implementationVersionManifest = attrs.getValue("Implementation-Version");
                }
            }

            if (claseEntry != null) {
                try (InputStream is = zip.getInputStream(claseEntry)) {
                    claseMajorVersion = leerMajorVersionClase(is);
                }
            }
        }

        if (info == null) {
            info = new PomInfo();
        }

        if (springBootVersionManifest != null) {
            info.springBootVersion = springBootVersionManifest;
        } else if (info.springBootVersion == null && springBootVersionLib != null) {
            info.springBootVersion = springBootVersionLib;
        }

        if (info.version == null && implementationVersionManifest != null) {
            info.version = implementationVersionManifest;
        }

        if (info.javaVersion == null && claseMajorVersion != null) {
            info.javaVersion = mapearMajorVersionAJava(claseMajorVersion);
        }

        return info;
    }

    private Integer leerMajorVersionClase(InputStream is) throws Exception {

        byte[] cabecera = new byte[8];
        int leidos = 0;

        while (leidos < cabecera.length) {

            int r = is.read(cabecera, leidos, cabecera.length - leidos);

            if (r == -1) {
                break;
            }

            leidos += r;
        }

        if (leidos < cabecera.length) {
            return null;
        }

        return ((cabecera[6] & 0xFF) << 8) | (cabecera[7] & 0xFF);
    }

    private String mapearMajorVersionAJava(int major) {

        if (major >= 53) {
            return String.valueOf(major - 44);
        }

        switch (major) {
            case 52: return "1.8";
            case 51: return "1.7";
            case 50: return "1.6";
            case 49: return "1.5";
            case 48: return "1.4";
            case 47: return "1.3";
            case 46: return "1.2";
            case 45: return "1.1";
            default: return null;
        }
    }

    private PomInfo extraerPomInfo(Element root) {

        PomInfo info = new PomInfo();

        info.artifactId = textoHijoDirecto(root, "artifactId");
        info.version = textoHijoDirecto(root, "version");
        info.packaging = textoHijoDirecto(root, "packaging");

        if (info.packaging == null || info.packaging.trim().isEmpty()) {
            info.packaging = "jar";
        }

        Element parent = primerHijo(root, "parent");

        if (parent != null) {

            String parentArtifact = textoHijoDirecto(parent, "artifactId");

            if (info.version == null) {
                info.version = textoHijoDirecto(parent, "version");
            }

            if (parentArtifact != null && parentArtifact.toLowerCase().contains("spring-boot")) {
                info.springBootVersion = textoHijoDirecto(parent, "version");
            }
        }

        Element properties = primerHijo(root, "properties");

        if (properties != null) {

            info.javaVersion = textoHijoDirecto(properties, "java.version");

            if (info.javaVersion == null) {
                info.javaVersion = textoHijoDirecto(properties, "maven.compiler.source");
            }
        }

        return info;
    }

    private Element primerHijo(Element padre, String tagName) {

        NodeList hijos = padre.getChildNodes();

        for (int i = 0; i < hijos.getLength(); i++) {

            if (hijos.item(i) instanceof Element) {

                Element el = (Element) hijos.item(i);

                if (tagName.equals(el.getTagName())) {
                    return el;
                }
            }
        }

        return null;
    }

    private String textoHijoDirecto(Element padre, String tagName) {

        Element hijo = primerHijo(padre, tagName);

        if (hijo == null) {
            return null;
        }

        String texto = hijo.getTextContent();

        return (texto == null || texto.trim().isEmpty()) ? null : texto.trim();
    }

    /**
     * Datos leídos directamente de un pom.xml ({@link #leerPom(File)}),
     * mirando solo los hijos directos de {@code <project>} (no usa
     * {@code getElementsByTagName} sobre todo el documento para no
     * confundirse con, por ejemplo, el {@code <version>} de una
     * dependencia).
     */
    private static final class PomInfo {

        private String artifactId;
        private String version;
        private String packaging;
        private String javaVersion;
        private String springBootVersion;
    }
}