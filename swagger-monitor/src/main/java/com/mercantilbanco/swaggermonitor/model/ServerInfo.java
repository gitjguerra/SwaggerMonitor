package com.mercantilbanco.swaggermonitor.model;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;

/**
 * CLASE ENCARGADA DE: <strong>campos a mostrar por server</strong>.
 * @author <strong>JOSE GUERRA</strong>
 * <hr><code>Copyright 20/05/2026. Banco Mercantil, All rights reserved.</code>
 */
@Getter
@Setter
public class ServerInfo {

    private String name;
    private String host;
    private int port;

    private boolean up;
    private long responseTime;
    private Date lastCheck;
    private boolean monitoring;
    private boolean firstCheck = true;
    private String error;
    private boolean slow;
    private String slackWebhook;

}