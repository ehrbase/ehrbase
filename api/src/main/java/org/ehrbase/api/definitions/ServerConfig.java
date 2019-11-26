package org.ehrbase.api.definitions;

public interface ServerConfig {

    int getPort();

    void setPort(int port);

    String getNodename();

    void setNodename(String nodename);
}
