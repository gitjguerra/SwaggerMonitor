package com.mercantilbanco.swaggermonitor.model;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;

/**
 * CLASE ENCARGADA DE: <strong>Historico del status de servidores</strong>.
 * @author <strong>JOSE GUERRA</strong>
 * <hr><code>Copyright 20/05/2026. Banco Mercantil, All rights reserved.</code>
 */

@Getter
@Setter
public class ServerHistory {

    private String name;
    private boolean up;
    private long responseTime;
    private Date timestamp;
    
}
