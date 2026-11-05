package com.mercantilbanco.swaggermonitor.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * CLASE ENCARGADA DE: <strong>XXX</strong>.
 * @author <strong>JOSE GUERRA</strong>
 * <hr><code>Copyright 05/11/2026. Banco Mercantil, All rights reserved.</code>
 */
@Getter
@Setter
public class ServiceFichaDTO {

    // Identificación
    private String servicio;
    private String dominio;
    private String version;

    // Impacto
    private int consumidores;
    private int dependencias;
    private int impactoPct;
    private String criticidad;
    private String cobertura;

    // Técnica
    private String javaVersion;
    private String springBootVersion;
    private String artifactId;
    private String packaging;

    // Relaciones
    private List<String> consume = new ArrayList<>();
    private List<String> impactados = new ArrayList<>();
}