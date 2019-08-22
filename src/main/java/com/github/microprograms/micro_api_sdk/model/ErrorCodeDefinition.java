package com.github.microprograms.micro_api_sdk.model;

import java.io.Serializable;

public class ErrorCodeDefinition implements Serializable {
	private static final long serialVersionUID = 1L;

    private String name;
    private int code;
    private String message;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
