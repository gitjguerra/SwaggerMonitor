package com.mercantilbanco.monitor.model;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;

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

}