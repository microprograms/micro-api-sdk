package com.github.microprograms.micro_api_sdk.model;

import java.io.Serializable;

public class ShowdocDefinition implements Serializable {
	private static final long serialVersionUID = 1L;

    private String apiKey;
    private String apiToken;
    private String url;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
