package com.mercantilbanco.swaggermonitor.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import com.mercantilbanco.swaggermonitor.model.Alert;
import com.mercantilbanco.swaggermonitor.model.ServerHistory;
import com.mercantilbanco.swaggermonitor.model.ServerInfo;
import com.mercantilbanco.swaggermonitor.service.ServerMonitorService;

@Controller
public class SwaggerMonitorController {

    @Autowired
    private ServerMonitorService service;

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("data", service.checkAll());
        return "dashboard";
    }

    @GetMapping("/api/history/{name}")
    @ResponseBody
    public List<ServerHistory> getHistory(@PathVariable String name) {
        return service.getHistory(name);
    }

    @GetMapping("/api/metrics/{name}")
    @ResponseBody
    public Map<String, Object> getMetrics(@PathVariable String name) {

        Map<String, Object> m = new HashMap<>();
        m.put("uptime", service.getUptime(name));
        m.put("downtime", service.getDownTime(name));

        return m;
    }

    @GetMapping("/api/alerts")
    @ResponseBody
    public List<Alert> getAlerts() {

        List<Alert> alerts = new ArrayList<>();

        Map<String, List<ServerInfo>> data = service.getCurrentStatus();
        if (data == null) return alerts;

        for (List<ServerInfo> servers : data.values()) {
            if (servers == null) continue;

            for (ServerInfo srv : servers) {

                if (srv == null) continue;

                if (!srv.isUp()) {
                    alerts.add(new Alert(srv.getName(), "DOWN", "Servidor caído", System.currentTimeMillis()));
                } else if (srv.getResponseTime() > 1000) {
                    alerts.add(new Alert(srv.getName(), "SLOW",
                            "Respuesta lenta (" + srv.getResponseTime() + " ms)",
                            System.currentTimeMillis()));
                }

                if (srv.getError() != null && !srv.getError().isEmpty()) {
                    alerts.add(new Alert(srv.getName(), "ERROR", srv.getError(), System.currentTimeMillis()));
                }
            }
        }

        return alerts;
    }

    @GetMapping("/api/events")
    @ResponseBody
    public List<Map<String, Object>> getEvents() {

        List<Map<String, Object>> events = new ArrayList<>();

        Map<String, List<ServerHistory>> history = service.getAllHistory();
        if (history == null) return events;

        for (String server : history.keySet()) {

            List<ServerHistory> list = history.get(server);
            if (list == null) continue;

            for (ServerHistory h : list) {

                if (h == null) continue;

                Map<String, Object> e = new HashMap<>();
                e.put("server", server);
                e.put("time", h.getTimestamp());

                if (!h.isUp()) {
                    e.put("type", "DOWN");
                    e.put("message", "Servidor se cayó");
                } else if (h.getResponseTime() > 1000) {
                    e.put("type", "SLOW");
                    e.put("message", "Se detectó lentitud");
                } else {
                    e.put("type", "RECOVER");
                    e.put("message", "Servidor recuperado");
                }

                events.add(e);
            }
        }

        return events;
    }

    @GetMapping("/api/chart/{name}")
    @ResponseBody
    public Map<String, Object> getChartByServer(@PathVariable String name) {

        Map<String, Object> result = new HashMap<>();

        List<ServerHistory> list = service.getHistory(name);

        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();

        for (ServerHistory h : list) {
            labels.add(new java.text.SimpleDateFormat("HH:mm:ss").format(h.getTimestamp()));
            values.add(h.isUp() ? h.getResponseTime() : 1800);
        }

        result.put("labels", labels);
        result.put("values", values);

        return result;
    }

    @GetMapping("/api/servers")
    @ResponseBody
    public List<String> getServers() {

        List<String> list = new ArrayList<>();

        Map<String, List<ServerInfo>> data = service.getCurrentStatus();
        if (data == null) return list;

        for (List<ServerInfo> servers : data.values()) {
            for (ServerInfo s : servers) {
                list.add(s.getName());
            }
        }

        return list;
    }

    @GetMapping("/api/swagger/files")
    public ResponseEntity<List<String>> getSwaggerFiles() {

        java.util.Set<String> result = new java.util.LinkedHashSet<>();

        try {

            String swaggerDir = getSwaggerDir();
            File folder = new File(swaggerDir);

            if (folder.exists() && folder.isDirectory()) {
                for (File file : folder.listFiles()) {
                    if (file.getName().endsWith(".json")) {
                        result.add(file.getName().replace(".json", "").toUpperCase());
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        List<String> sorted = new ArrayList<>(result);
        java.util.Collections.sort(sorted);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(sorted);
    }

    @GetMapping("/swagger/{name}.json")
    @ResponseBody
    public ResponseEntity<String> getSwagger(@PathVariable String name) {

        try {

            String swaggerDir = getSwaggerDir();

            File file = findSwaggerFileIgnoreCase(swaggerDir, name);

            if (file != null && file.exists()) {

                String content = new String(
                        java.nio.file.Files.readAllBytes(file.toPath()),
                        java.nio.charset.StandardCharsets.UTF_8
                );

                return ResponseEntity.ok()
                        .header("Content-Type", "application/json;charset=UTF-8")
                        .body(content);
            }

            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    private File findSwaggerFileIgnoreCase(String dir, String name) {

        File folder = new File(dir);
        if (!folder.exists() || !folder.isDirectory()) return null;

        File[] files = folder.listFiles();
        if (files == null) return null;

        String target = name + ".json";

        for (File f : files) {
            if (f.getName().equalsIgnoreCase(target)) {
                return f;
            }
        }

        return null;
    }

    private String getSwaggerDir() {

        String dir = System.getProperty("pathSwaggerOutput");

        if (dir == null || dir.trim().isEmpty()) {
            throw new RuntimeException("❌ pathSwaggerOutput no configurado");
        }

        return dir;
    }

    @GetMapping("/api/servers/status")
    @ResponseBody
    public List<Map<String, Object>> getServersStatus() {

        List<Map<String, Object>> result = new ArrayList<>();

        Map<String, List<ServerInfo>> data = service.getCurrentStatus();
        if (data == null) return result;

        for (List<ServerInfo> servers : data.values()) {
            for (ServerInfo srv : servers) {

                Map<String, Object> m = new HashMap<>();
                m.put("name", srv.getName());
                m.put("up", srv.isUp());
                m.put("responseTime", srv.getResponseTime());

                result.add(m);
            }
        }

        return result;
    }
}