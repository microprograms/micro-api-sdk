package com.github.microprograms.micro_api_sdk.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.github.microprograms.micro_api_sdk.model.ApiDefinition;
import com.github.microprograms.micro_api_sdk.model.ModuleDefinition;
import com.github.microprograms.micro_api_sdk.model.PlainEntityDefinition;
import com.github.microprograms.micro_api_sdk.model.PlainFieldDefinition;
import com.github.microprograms.micro_api_sdk.model.PlainModelDefinition;

public class FieldMetas {
	private List<FieldMeta> list = new ArrayList<>();

	public FieldMeta get(String name) {
		for (FieldMeta x : list) {
			if (x.getName().equals(name)) {
				return x;
			}
		}
		return null;
	}

	public String getComment(String name, String defaultValue) {
		FieldMeta fieldMeta = get(name);
		if (fieldMeta == null) {
			return defaultValue;
		}
		return StringUtils.isBlank(fieldMeta.getComment()) ? defaultValue : fieldMeta.getComment();
	}

	public Object getExample(String name) {
		FieldMeta fieldMeta = get(name);
		if (fieldMeta == null) {
			return null;
		}
		return fieldMeta.getExample();
	}

	public void add(FieldMeta fieldMeta) {
		list.add(fieldMeta);
	}

	public List<FieldMeta> getAll() {
		return list;
	}

	public static FieldMetas parseRequestFieldMetas(String configFilePath, String apiName) throws Exception {
		return parseRequestFieldMetas(ApiSdk.build(configFilePath), apiName);
	}

	public static FieldMetas parseRequestFieldMetas(ModuleDefinition moduleDefinition, String apiName)
			throws Exception {
		for (ApiDefinition x : moduleDefinition.getApiDefinitions()) {
			if (x.getName().equals(apiName)) {
				return parseRequestFieldMetas(x);
			}
		}
		return null;
	}

	public static FieldMetas parseRequestFieldMetas(ApiDefinition apiDefinition) {
		FieldMetas fieldMetas = new FieldMetas();
		for (PlainFieldDefinition x : apiDefinition.getRequestDefinition().getFieldDefinitions()) {
			fieldMetas.add(new FieldMeta(x.getName(), x.getComment(), x.getExample()));
		}
		return fieldMetas;
	}

	public static FieldMetas parseResponseFieldMetas(String configFilePath, String apiName) throws Exception {
		return parseResponseFieldMetas(ApiSdk.build(configFilePath), apiName);
	}

	public static FieldMetas parseResponseFieldMetas(ModuleDefinition moduleDefinition, String apiName)
			throws Exception {
		for (ApiDefinition x : moduleDefinition.getApiDefinitions()) {
			if (x.getName().equals(apiName)) {
				return parseResponseFieldMetas(x);
			}
		}
		return null;
	}

	public static FieldMetas parseResponseFieldMetas(ApiDefinition apiDefinition) {
		FieldMetas fieldMetas = new FieldMetas();
		for (PlainFieldDefinition x : apiDefinition.getResponseDefinition().getFieldDefinitions()) {
			fieldMetas.add(new FieldMeta(x.getName(), x.getComment(), x.getExample()));
		}
		return fieldMetas;
	}

	public static FieldMetas parseMicroModelFieldMetas(String configFilePath, String modelJavaClassName)
			throws Exception {
		return parseMicroModelFieldMetas(ModelSdk.build(configFilePath), modelJavaClassName);
	}

	public static FieldMetas parseMicroModelFieldMetas(PlainModelDefinition modelDefinition,
			String modelJavaClassName) {
		for (PlainEntityDefinition x : modelDefinition.getEntityDefinitions()) {
			if (x.getName().equals(modelJavaClassName)) {
				return parseMicroModelFieldMetas(x);
			}
		}
		return null;
	}

	public static FieldMetas parseMicroModelFieldMetas(PlainEntityDefinition entityDefinition) {
		FieldMetas fieldMetas = new FieldMetas();
		for (PlainFieldDefinition x : entityDefinition.getFieldDefinitions()) {
			fieldMetas.add(new FieldMeta(x.getName(), x.getComment(), x.getExample()));
		}
		return fieldMetas;
	}

	/**
	 * 字段元数据
	 */
	public static class FieldMeta {
		/**
		 * 字段名
		 */
		private String name;
		/**
		 * 注释
		 */
		private String comment;
		/**
		 * 示例值
		 */
		private Object example;

		public FieldMeta(String name, String comment, Object example) {
			this.name = name;
			this.comment = comment;
			this.example = example;
		}

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

		public Object getExample() {
			return example;
		}

		public void setExample(Object example) {
			this.example = example;
		}
	}
}
