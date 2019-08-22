package com.github.microprograms.micro_api_sdk.model;

import java.io.Serializable;
import java.util.List;

import com.github.microprograms.micro_model_sdk.model.PlainEntityDefinition;

public class ApiServerDefinition implements Serializable {
	private static final long serialVersionUID = 1L;

	private String comment;
	private String description;
	private String version;
	private String javaPackageName;
	private List<ApiDefinition> apiDefinitions;
	private List<ErrorCodeDefinition> errorCodeDefinitions;
	private List<PlainEntityDefinition> modelDefinitions;
	private List<MixinDefinition> mixinDefinitions;
	private ServerAddressDefinition serverAddressDefinition;
	private ShowdocDefinition showdocDefinition;
	private DeployDefinition deployDefinition;
	private List<ChangeLog> changeLogs;

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

	public List<PlainEntityDefinition> getModelDefinitions() {
		return modelDefinitions;
	}

	public void setModelDefinitions(List<PlainEntityDefinition> modelDefinitions) {
		this.modelDefinitions = modelDefinitions;
	}

	public List<MixinDefinition> getMixinDefinitions() {
		return mixinDefinitions;
	}

	public void setMixinDefinitions(List<MixinDefinition> mixinDefinitions) {
		this.mixinDefinitions = mixinDefinitions;
	}

	public ServerAddressDefinition getServerAddressDefinition() {
		return serverAddressDefinition;
	}

	public void setServerAddressDefinition(ServerAddressDefinition serverAddressDefinition) {
		this.serverAddressDefinition = serverAddressDefinition;
	}

	public ShowdocDefinition getShowdocDefinition() {
		return showdocDefinition;
	}

	public void setShowdocDefinition(ShowdocDefinition showdocDefinition) {
		this.showdocDefinition = showdocDefinition;
	}

	public DeployDefinition getDeployDefinition() {
		return deployDefinition;
	}

	public void setDeployDefinition(DeployDefinition deployDefinition) {
		this.deployDefinition = deployDefinition;
	}

	public List<ChangeLog> getChangeLogs() {
		return changeLogs;
	}

	public void setChangeLogs(List<ChangeLog> changeLogs) {
		this.changeLogs = changeLogs;
	}
}
