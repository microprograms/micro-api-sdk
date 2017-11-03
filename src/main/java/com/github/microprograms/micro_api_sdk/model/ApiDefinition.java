package com.github.microprograms.micro_api_sdk.model;

import java.util.List;

import com.github.microprograms.micro_entity_definition_runtime.model.EntityDefinition;

public class ApiDefinition {
    private String type;
    private String comment;
    private String description;
    private List<String> imports;
    private String javaClassName;
    private EntityDefinition requestDefinition;
    private EntityDefinition responseDefinition;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getImports() {
        return imports;
    }

    public void setImports(List<String> imports) {
        this.imports = imports;
    }

    public String getJavaClassName() {
        return javaClassName;
    }

    public void setJavaClassName(String javaClassName) {
        this.javaClassName = javaClassName;
    }

    public EntityDefinition getRequestDefinition() {
        return requestDefinition;
    }

    public void setRequestDefinition(EntityDefinition requestDefinition) {
        this.requestDefinition = requestDefinition;
    }

    public EntityDefinition getResponseDefinition() {
        return responseDefinition;
    }

    public void setResponseDefinition(EntityDefinition responseDefinition) {
        this.responseDefinition = responseDefinition;
    }
}
