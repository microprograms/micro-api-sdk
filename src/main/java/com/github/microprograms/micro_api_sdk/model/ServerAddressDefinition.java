package com.github.microprograms.micro_api_sdk.model;

public class ServerAddressDefinition {
    private String host;
    private int port;
    private String url;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
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
        return String.format("http://%s:%s%s", host, port, url);
    }
}
