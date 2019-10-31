package com.github.microprograms.micro_api_sdk.model;

import java.io.Serializable;
import java.util.Map;

public class ApiDefinition implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 唯一标识符
	 */
	private String name;
	private String comment;
	private String description;
	private PlainEntityDefinition requestDefinition;
	private PlainEntityDefinition responseDefinition;
	private Map<String, Object> ext;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public PlainEntityDefinition getRequestDefinition() {
		return requestDefinition;
	}

	public void setRequestDefinition(PlainEntityDefinition requestDefinition) {
		this.requestDefinition = requestDefinition;
	}

	public PlainEntityDefinition getResponseDefinition() {
		return responseDefinition;
	}

	public void setResponseDefinition(PlainEntityDefinition responseDefinition) {
		this.responseDefinition = responseDefinition;
	}

	public Map<String, Object> getExt() {
		return ext;
	}

	public void setExt(Map<String, Object> ext) {
		this.ext = ext;
	}
}
