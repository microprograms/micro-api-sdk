package com.github.microprograms.micro_api_sdk.model;

import java.io.Serializable;

public class ServerAddressDefinition implements Serializable {
    private static final long serialVersionUID = 1L;

    private String scheme = "http";
    private String host = "localhost";
    private Integer port;
    private String url;

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s://%s", scheme, host));
        if (port != null) {
            sb.append(":").append(port);
        }
        if (url != null) {
            sb.append(url);
        }
        return sb.toString();
    }
}
