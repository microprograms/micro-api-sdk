package com.github.microprograms.micro_api_sdk.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class PlainEntityDefinition implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 唯一标识符
	 */
	private String name;
	private String comment;
	private String description;
	private List<PlainFieldDefinition> fieldDefinitions;
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

	public List<PlainFieldDefinition> getFieldDefinitions() {
		return fieldDefinitions;
	}

	public void setFieldDefinitions(List<PlainFieldDefinition> fieldDefinitions) {
		this.fieldDefinitions = fieldDefinitions;
	}

	public Map<String, Object> getExt() {
		return ext;
	}

	public void setExt(Map<String, Object> ext) {
		this.ext = ext;
	}
}
