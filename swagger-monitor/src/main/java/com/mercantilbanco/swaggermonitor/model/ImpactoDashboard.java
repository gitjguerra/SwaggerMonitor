package com.mercantilbanco.swaggermonitor.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * CLASE ENCARGADA DE: <strong>KPIs ejecutivos del análisis de impacto / dependencias</strong>.
 * @author <strong>JOSE GUERRA</strong>
 * <hr><code>Copyright 02/11/2026. Banco Mercantil, All rights reserved.</code>
 */
@Getter
@Setter
public class ImpactoDashboard {

    private String microservicio;
    private int totalDominiosAnalizados;
    private List<String> dominios = new ArrayList<>();
    private int totalServiciosRestservices;
    private int totalServiciosConCoincidencia;
    private double coberturaPct;
    private int serviciosCriticos;
    private int serviciosAltos;
    private int spofDetectados;
    private boolean usandoDatosFallback;
}
