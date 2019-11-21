package org.ehrbase.application.config;

import org.apache.commons.lang.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Configuration
@ConfigurationProperties(prefix = "server")
public class ServerConfigImp implements org.ehrbase.api.definitions.ServerConfig {

    @Min(1025)
    @Max(65536)
    private int port;
    private String nodename;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getNodename() {
        if (StringUtils.isBlank(nodename))
            return "local.ehrbase.org";
        return nodename;
    }

    public void setNodename(String nodename) {
        this.nodename = nodename;
    }
}
