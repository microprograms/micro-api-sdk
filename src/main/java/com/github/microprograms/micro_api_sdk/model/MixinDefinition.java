package com.github.microprograms.micro_api_sdk.model;

import java.io.Serializable;

public class MixinDefinition implements Serializable {
	private static final long serialVersionUID = 1L;

    private String target;
    private String source;

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
