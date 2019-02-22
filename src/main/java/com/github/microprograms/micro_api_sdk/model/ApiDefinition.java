package com.github.microprograms.micro_api_sdk.model;

import java.util.List;
import java.util.Map;

import com.github.microprograms.micro_nested_data_model_sdk.model.NestedEntityDefinition;

public class ApiDefinition {
	private String type;
	private String comment;
	private String description;
	private List<String> imports;
	private String javaClassName;
	private NestedEntityDefinition requestDefinition;
	private NestedEntityDefinition responseDefinition;
	private Map<String, Object> ext;

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

	public NestedEntityDefinition getRequestDefinition() {
		return requestDefinition;
	}

	public void setRequestDefinition(NestedEntityDefinition requestDefinition) {
		this.requestDefinition = requestDefinition;
	}

	public NestedEntityDefinition getResponseDefinition() {
		return responseDefinition;
	}

	public void setResponseDefinition(NestedEntityDefinition responseDefinition) {
		this.responseDefinition = responseDefinition;
	}

	public Map<String, Object> getExt() {
		return ext;
	}

	public void setExt(Map<String, Object> ext) {
		this.ext = ext;
	}
}
