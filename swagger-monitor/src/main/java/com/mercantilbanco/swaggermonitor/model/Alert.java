package com.mercantilbanco.swaggermonitor.model;

import lombok.Getter;
import lombok.Setter;

/**
 * CLASE ENCARGADA DE: <strong>XXX</strong>.
 * @author <strong>JOSE GUERRA</strong>
 * <hr><code>Copyright 21/05/2026. Banco Mercantil, All rights reserved.</code>
 */
@Getter
@Setter
public class Alert {

	public String server;
    public String level;
    public String message;
    public long timestamp;

    public Alert(String server, String level, String message, long timestamp) {
        this.server = server;
        this.level = level;
        this.message = message;
        this.timestamp = timestamp;
    }

}
