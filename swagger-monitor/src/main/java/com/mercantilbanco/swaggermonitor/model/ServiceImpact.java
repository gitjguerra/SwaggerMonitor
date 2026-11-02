package com.mercantilbanco.swaggermonitor.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * CLASE ENCARGADA DE: <strong>representar el impacto de un servicio de restservices.xml sobre los dominios de negocio</strong>.
 * @author <strong>JOSE GUERRA</strong>
 * <hr><code>Copyright 02/11/2026. Banco Mercantil, All rights reserved.</code>
 */
@Getter
@Setter
public class ServiceImpact {

    private String name;
    private String endpoint;
    private List<String> domains = new ArrayList<>();
    private int cantidad;
    private int pct;
    private String criticidad;
}
