package com.github.microprograms.micro_api_sdk.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSON;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.microprograms.micro_api_sdk.model.PlainEntityDefinition;
import com.github.microprograms.micro_api_sdk.model.PlainEntityRefDefinition;
import com.github.microprograms.micro_api_sdk.model.PlainFieldDefinition;
import com.github.microprograms.micro_api_sdk.model.PlainModelDefinition;
import com.github.microprograms.micro_oss_core.model.FieldDefinition;
import com.github.microprograms.micro_oss_core.model.FieldDefinition.FieldTypeEnum;
import com.github.microprograms.micro_oss_core.model.TableDefinition;
import com.github.microprograms.micro_oss_core.model.ddl.CreateTableCommand;
import com.github.microprograms.micro_oss_core.model.ddl.DropTableCommand;
import com.github.microprograms.micro_oss_mysql.utils.MysqlUtils;
import com.github.microprograms.micro_refs.utils.MicroRefsUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * 建模（表结构定义）
 */
public class ModelSdk {
	public static final String enum_field_class_name_format = "%sEnum";
	private static final Charset encoding = Charset.forName("utf8");

	public static PlainModelDefinition build(String configFilePath) throws IOException {
		String json = Fn.readFile(configFilePath, encoding);
		return JSON.parseObject(json, PlainModelDefinition.class);
	}

	/**
	 * sql初始化脚本
	 */
	public static class Sql {

		/**
		 * 把sql写到文件
		 * 
		 * @param modelDefinition
		 * @param excludeModelNames
		 * @param tablePrefix
		 * @param javaPackageName
		 * @param dir
		 * @throws Exception
		 */
		public static void writeToFile(PlainModelDefinition modelDefinition, List<String> excludeModelNames,
				String tablePrefix, String javaPackageName, File dir) throws Exception {
			String data = new SimpleDateFormat("yyyyMMdd").format(new Date());
			String ver = modelDefinition.getVersion().replaceFirst("^v", "");
			String filename = String.format("init-v%s-%s.sql", ver, data);
			String sql = buildInitSql(modelDefinition, excludeModelNames, tablePrefix, javaPackageName);
			FileUtils.writeStringToFile(new File(dir, filename), sql, encoding);
		}

		/**
		 * 构建sql初始化脚本
		 * 
		 * @param modelDefinition
		 * @param excludeModelNames
		 * @param tablePrefix
		 * @param javaPackageName
		 * @return
		 * @throws Exception
		 */
		public static String buildInitSql(PlainModelDefinition modelDefinition, List<String> excludeModelNames,
				String tablePrefix, String javaPackageName) throws Exception {
			StringBuffer sb = new StringBuffer();
			for (PlainEntityDefinition x : modelDefinition.getEntityDefinitions()) {
				if (excludeModelNames.contains(x.getName())) {
					continue;
				}
				sb.append(String.format("# Dump of table %s（%s）\n", x.getComment(), x.getName()));
				sb.append("# ------------------------------------------------------------\n\n");
				sb.append(MysqlUtils.buildSql(new DropTableCommand(_getTableName(x, tablePrefix)))).append("\n\n");
				sb.append(MysqlUtils.buildSql(new CreateTableCommand(_buildTableDefinition(x, tablePrefix))))
						.append("\n\n");
			}
			for (PlainEntityRefDefinition x : modelDefinition.getEntityRefDefinitions()) {
				Class<?> sourceClz = _getEntityClass(x.getSource().getName(), javaPackageName);
				Class<?> targetClz = _getEntityClass(x.getTarget().getName(), javaPackageName);
				sb.append(String.format("# Dump of ref table（%s and %s）\n", sourceClz.getSimpleName(),
						targetClz.getSimpleName()));
				sb.append("# ------------------------------------------------------------\n\n");
				sb.append(MysqlUtils.buildSql(MicroRefsUtils.buildDropTableCommand(sourceClz, targetClz, tablePrefix)))
						.append("\n\n");
				sb.append(
						MysqlUtils.buildSql(MicroRefsUtils.buildCreateTableCommand(sourceClz, targetClz, tablePrefix)))
						.append("\n\n");
			}
			return sb.toString();
		}

		private static Class<?> _getEntityClass(String name, String javaPackageName) throws ClassNotFoundException {
			return Class.forName(javaPackageName + name);
		}

		private static TableDefinition _buildTableDefinition(PlainEntityDefinition entityDefinition,
				String tablePrefix) {
			List<FieldDefinition> fields = new ArrayList<>();
			for (PlainFieldDefinition x : entityDefinition.getFieldDefinitions()) {
				String fieldName = _getFieldName(x);
				fields.add(new FieldDefinition(fieldName, x.getComment(), FieldTypeEnum.parse(x.getJavaType()),
						x.getDefaultValue(), x.getPrimaryKey()));
			}
			String tableName = _getTableName(entityDefinition, tablePrefix);
			return new TableDefinition(tableName, entityDefinition.getComment(), fields);
		}

		private static String _getTableName(PlainEntityDefinition entityDefinition, String tablePrefix) {
			if (StringUtils.isBlank(tablePrefix)) {
				return entityDefinition.getName();
			}
			return tablePrefix + entityDefinition.getName();
		}

		private static String _getFieldName(PlainFieldDefinition fieldDefinition) {
			return fieldDefinition.getName();
		}
	}

	/**
	 * 更新Model实体类
	 */
	public static class UpdateJavaSourceFile {

		/**
		 * 覆盖更新全部Model实体类
		 * 
		 * @param modelDefinition
		 * @param srcFolder
		 * @param javaPackageName
		 * @throws Exception
		 */
		public static void updateAll(PlainModelDefinition modelDefinition, String srcFolder, String javaPackageName)
				throws Exception {
			for (PlainEntityDefinition x : modelDefinition.getEntityDefinitions()) {
				update(x, srcFolder, javaPackageName);
			}
		}

		/**
		 * 覆盖更新单个Model实体类
		 * 
		 * @param entityDefinition
		 * @param srcFolder
		 * @param javaPackageName
		 * @throws IOException
		 */
		public static void update(PlainEntityDefinition entityDefinition, String srcFolder, String javaPackageName)
				throws IOException {
			String entityJavaClassName = entityDefinition.getName();
			File javaFile = JavaParserUtils.buildJavaSourceFile(srcFolder, javaPackageName, entityJavaClassName);
			CompilationUnit cu = null;
			if (javaFile.exists()) {
				cu = JavaParser.parse(javaFile, encoding);
				ClassOrInterfaceDeclaration entityClassDeclaration = cu.getClassByName(entityJavaClassName).get();
				entityClassDeclaration.getMembers().clear();
				for (PlainFieldDefinition x : entityDefinition.getFieldDefinitions()) {
					fillField(entityClassDeclaration, x);
				}
			} else {
				javaFile.getParentFile().mkdirs();
				javaFile.createNewFile();
				cu = new CompilationUnit(javaPackageName);
				ClassOrInterfaceDeclaration modelClassDeclaration = cu.addClass(entityDefinition.getName(),
						Modifier.PUBLIC);
				modelClassDeclaration.setComment(new JavadocComment("\n * " + entityDefinition.getComment() + "\n"));
				fillFields(modelClassDeclaration, entityDefinition);
			}
			JavaParserUtils.write(cu, javaFile, encoding);
		}

		private static void fillFields(ClassOrInterfaceDeclaration classDeclaration,
				PlainEntityDefinition entityDefinition) {
			for (PlainFieldDefinition x : entityDefinition.getFieldDefinitions()) {
				fillField(classDeclaration, x);
			}
		}

		private static void fillField(ClassOrInterfaceDeclaration classDeclaration,
				PlainFieldDefinition fieldDefinition) {
			String type = fieldDefinition.getJavaType();
			FieldDeclaration fieldDeclaration = new FieldDeclaration(EnumSet.of(Modifier.PRIVATE),
					new VariableDeclarator(new ClassOrInterfaceType(type), fieldDefinition.getName()));
			classDeclaration.addMember(fieldDeclaration);
			fieldDeclaration.setLineComment(" " + fieldDefinition.getComment());
			fieldDeclaration.createGetter();
			fieldDeclaration.createSetter();

			fillEnumField(classDeclaration, fieldDefinition);
		}

		private static void fillEnumField(ClassOrInterfaceDeclaration classDeclaration,
				PlainFieldDefinition fieldDefinition) {
			Pattern pattern = Pattern.compile("[(（]([A-Za-z0-9_]+[:：][^,，:：)）]+[,，]?)+[)）]");
			Matcher matcher = pattern.matcher(fieldDefinition.getComment());
			if (!matcher.find()) {
				return;
			}
			String enumDefinition = matcher.group();
			String[] pairs = enumDefinition.substring(1, enumDefinition.length() - 1).split("[,，]");

			EnumDeclaration enumDeclaration = new EnumDeclaration(EnumSet.of(Modifier.PUBLIC),
					String.format(enum_field_class_name_format, StringUtils.capitalize(fieldDefinition.getName())));
			classDeclaration.addMember(enumDeclaration);
			for (String pair : pairs) {
				if (StringUtils.isBlank(pair)) {
					return;
				}
				String[] kv = pair.split("[:：]");
				enumDeclaration.addEnumConstant(kv[0]).setJavadocComment(kv[1]);
			}
		}
	}
}
