package com.github.microprograms.micro_api_sdk.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class PlainModelDefinition implements Serializable {
	private static final long serialVersionUID = 1L;

	private String comment;
	private String description;
	private String version;
	private List<PlainEntityDefinition> entityDefinitions;
	private Map<String, Object> ext;

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

	public List<PlainEntityDefinition> getEntityDefinitions() {
		return entityDefinitions;
	}

	public void setEntityDefinitions(List<PlainEntityDefinition> entityDefinitions) {
		this.entityDefinitions = entityDefinitions;
	}

	public Map<String, Object> getExt() {
		return ext;
	}

	public void setExt(Map<String, Object> ext) {
		this.ext = ext;
	}
}
