package com.mercantilbanco.swaggermonitor.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * CLASE ENCARGADA DE: <strong>Mostrar los campos del servidor monitoreado</strong>.
 * @author <strong>JOSE GUERRA</strong>
 * <hr><code>Copyright 21/05/2026. Banco Mercantil, All rights reserved.</code>
 */

@Getter
@AllArgsConstructor
public class Alert {

    private String server;
    private String level;
    private String message;
    private long timestamp;
}

