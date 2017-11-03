package com.github.microprograms.micro_api_sdk.model;

import java.util.List;

import com.github.microprograms.micro_entity_definition_runtime.model.EntityDefinition;

public class EngineDefinition {
    private String comment;
    private String description;
    private String version;
    private String javaPackageName;
    private List<ApiDefinition> apiDefinitions;
    private List<ErrorCodeDefinition> errorCodeDefinitions;
    private List<EntityDefinition> modelDefinitions;
    private ServerAddressDefinition serverAddressDefinition;

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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getJavaPackageName() {
        return javaPackageName;
    }

    public void setJavaPackageName(String javaPackageName) {
        this.javaPackageName = javaPackageName;
    }

    public List<ApiDefinition> getApiDefinitions() {
        return apiDefinitions;
    }

    public void setApiDefinitions(List<ApiDefinition> apiDefinitions) {
        this.apiDefinitions = apiDefinitions;
    }

    public List<ErrorCodeDefinition> getErrorCodeDefinitions() {
        return errorCodeDefinitions;
    }

    public void setErrorCodeDefinitions(List<ErrorCodeDefinition> errorCodeDefinitions) {
        this.errorCodeDefinitions = errorCodeDefinitions;
    }

    public List<EntityDefinition> getModelDefinitions() {
        return modelDefinitions;
    }

    public void setModelDefinitions(List<EntityDefinition> modelDefinitions) {
        this.modelDefinitions = modelDefinitions;
    }

    public ServerAddressDefinition getServerAddressDefinition() {
        return serverAddressDefinition;
    }

    public void setServerAddressDefinition(ServerAddressDefinition serverAddressDefinition) {
        this.serverAddressDefinition = serverAddressDefinition;
    }
}
