package com.github.microprograms.micro_api_sdk.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class PlainFieldDefinition implements Serializable {
	private static final long serialVersionUID = 1L;

	private String name;
	private String comment;
	private String description;
	private Object example;
	private List<Object> examples;
	private String javaType;
	private boolean required;
	private int primaryKey;
	private Object defaultValue;
	private String mock;
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

	public Object getExample() {
		return example;
	}

	public void setExample(Object example) {
		this.example = example;
	}

	public List<Object> getExamples() {
		return examples;
	}

	public void setExamples(List<Object> examples) {
		this.examples = examples;
	}

	public String getJavaType() {
		return javaType;
	}

	public void setJavaType(String javaType) {
		this.javaType = javaType;
	}

	public boolean getRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public int getPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(int primaryKey) {
		this.primaryKey = primaryKey;
	}

	public Object getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getMock() {
		return mock;
	}

	public void setMock(String mock) {
		this.mock = mock;
	}

	public Map<String, Object> getExt() {
		return ext;
	}

	public void setExt(Map<String, Object> ext) {
		this.ext = ext;
	}
}
