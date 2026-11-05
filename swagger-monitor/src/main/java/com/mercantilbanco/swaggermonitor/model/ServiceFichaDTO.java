package com.mercantilbanco.swaggermonitor.model;

import java.util.ArrayList;
import java.util.List;

/**
 * CLASE ENCARGADA DE: <strong>XXX</strong>.
 * @author <strong>JOSE GUERRA</strong>
 * <hr><code>Copyright 05/11/2026. Banco Mercantil, All rights reserved.</code>
 */
public class ServiceFichaDTO {

    // Identificación
    private String servicio;
    private String dominio;
    private String version;

    // Impacto
    private int endpoints;
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

    // Swagger
    private String swaggerUrl;

    // Relaciones
    private List<String> consume = new ArrayList<>();
    private List<String> impactados = new ArrayList<>();

    public String getServicio() {
        return servicio;
    }

    public void setServicio(String servicio) {
        this.servicio = servicio;
    }

    public String getDominio() {
        return dominio;
    }

    public void setDominio(String dominio) {
        this.dominio = dominio;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(int endpoints) {
        this.endpoints = endpoints;
    }

    public int getConsumidores() {
        return consumidores;
    }

    public void setConsumidores(int consumidores) {
        this.consumidores = consumidores;
    }

    public int getDependencias() {
        return dependencias;
    }

    public void setDependencias(int dependencias) {
        this.dependencias = dependencias;
    }

    public int getImpactoPct() {
        return impactoPct;
    }

    public void setImpactoPct(int impactoPct) {
        this.impactoPct = impactoPct;
    }

    public String getCriticidad() {
        return criticidad;
    }

    public void setCriticidad(String criticidad) {
        this.criticidad = criticidad;
    }

    public String getCobertura() {
        return cobertura;
    }

    public void setCobertura(String cobertura) {
        this.cobertura = cobertura;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    public String getSpringBootVersion() {
        return springBootVersion;
    }

    public void setSpringBootVersion(String springBootVersion) {
        this.springBootVersion = springBootVersion;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getPackaging() {
        return packaging;
    }

    public void setPackaging(String packaging) {
        this.packaging = packaging;
    }

    public String getSwaggerUrl() {
        return swaggerUrl;
    }

    public void setSwaggerUrl(String swaggerUrl) {
        this.swaggerUrl = swaggerUrl;
    }

    public List<String> getConsume() {
        return consume;
    }

    public void setConsume(List<String> consume) {
        this.consume = consume;
    }

    public List<String> getImpactados() {
        return impactados;
    }

    public void setImpactados(List<String> impactados) {
        this.impactados = impactados;
    }
}