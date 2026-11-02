package com.mercantilbanco.swaggermonitor.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * CLASE ENCARGADA DE: <strong>representar un dominio de negocio y los servicios de restservices.xml que dependen de él</strong>.
 * @author <strong>JOSE GUERRA</strong>
 * <hr><code>Copyright 02/11/2026. Banco Mercantil, All rights reserved.</code>
 */
@Getter
@Setter
public class DomainImpact {

    private String name;
    private int totalServiciosDominio;
    private List<ServiceImpact> serviciosDependientes = new ArrayList<>();
}
