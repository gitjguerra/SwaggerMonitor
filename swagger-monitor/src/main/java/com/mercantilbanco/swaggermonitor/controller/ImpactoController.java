package com.mercantilbanco.swaggermonitor.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.mercantilbanco.swaggermonitor.model.DomainImpact;
import com.mercantilbanco.swaggermonitor.model.ImpactoDashboard;
import com.mercantilbanco.swaggermonitor.model.ServiceFichaDTO;
import com.mercantilbanco.swaggermonitor.model.ServiceImpact;
import com.mercantilbanco.swaggermonitor.service.ImpactoService;


/**
 * CLASE ENCARGADA DE: <strong>exponer el analisis de impacto/dependencias (osba vs. dominios de negocio)
 * para la pestanhia "Impacto / Dependencias"</strong>.
 * @author <strong>JOSE GUERRA</strong>
 * <hr><code>Copyright 02/11/2026. Banco Mercantil, All rights reserved.</code>
 */
@RestController
public class ImpactoController {

    private final ImpactoService service;

    public ImpactoController(ImpactoService service) {
        this.service = service;
    }

    @GetMapping("/api/impacto/dashboard")
    public ImpactoDashboard getDashboard() {
        return service.getDashboard();
    }

    @GetMapping("/api/impacto/servicios")
    public List<ServiceImpact> getServicios() {
        return service.getRanking();
    }

    @GetMapping("/api/impacto/servicio/{name}")
    public ResponseEntity<ServiceImpact> getServicio(@PathVariable String name) {

        ServiceImpact servicio = service.getServicio(name);

        if (servicio == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(servicio);
    }

    @GetMapping("/api/impacto/ficha/{name}")
    public ResponseEntity<ServiceFichaDTO> getFichaTecnica(
            @PathVariable String name) {

        ServiceFichaDTO ficha = service.getFichaTecnica(name);

        if (ficha == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        ficha.setSwaggerUrl(baseUrl + ficha.getSwaggerUrl());

        return ResponseEntity.ok(ficha);
    }

    @GetMapping("/api/impacto/dominio/{nombre}")
    public ResponseEntity<DomainImpact> getDominio(
            @PathVariable String nombre) {

        DomainImpact dominio = service.getDominio(nombre);

        if (dominio == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(dominio);
    }
}
