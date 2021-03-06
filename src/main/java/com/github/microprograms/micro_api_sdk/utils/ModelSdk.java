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
import com.github.microprograms.micro_api_sdk.utils.MockUtils.PlainEntityMock;
import com.github.microprograms.micro_api_sdk.utils.MockUtils.PlainEntityRefMock;
import com.github.microprograms.micro_api_sdk.utils.MockUtils.PlainModelMock;
import com.github.microprograms.micro_api_sdk.utils.ModelSdk.UpdateJavaSourceFile.EnumFieldDefinition.Pair;
import com.github.microprograms.micro_oss_core.model.FieldDefinition;
import com.github.microprograms.micro_oss_core.model.FieldDefinition.FieldTypeEnum;
import com.github.microprograms.micro_oss_core.model.TableDefinition;
import com.github.microprograms.micro_oss_core.model.ddl.CreateTableCommand;
import com.github.microprograms.micro_oss_core.model.ddl.DropTableCommand;
import com.github.microprograms.micro_oss_core.model.dml.update.InsertCommand;
import com.github.microprograms.micro_oss_core.utils.MicroOssUtils;
import com.github.microprograms.micro_oss_mysql.utils.MysqlUtils;
import com.github.microprograms.micro_refs.model.Ref;
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

	public static Class<? extends Object> getEntityClass(String name, String javaPackageName)
			throws ClassNotFoundException {
		ClassLoader currentThreadClassLoader = Thread.currentThread().getContextClassLoader();
		return currentThreadClassLoader.loadClass(String.format("%s.%s", javaPackageName, name));
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
			_writeToFile(String.format("init-v%s-%s.sql", ver, data),
					buildInitSql(modelDefinition, excludeModelNames, tablePrefix, javaPackageName), dir);
			_writeToFile(String.format("mock-v%s-%s.sql", ver, data),
					buildMockSql(modelDefinition, excludeModelNames, tablePrefix, javaPackageName), dir);
		}

		private static void _writeToFile(String filename, String sql, File dir) throws IOException {
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

				DropTableCommand dropTableCommand = new DropTableCommand(x.getName());
				dropTableCommand.setTableName(
						MicroOssUtils.getTableNameWithPrefix(dropTableCommand.getTableName(), tablePrefix));
				sb.append(MysqlUtils.buildSql(dropTableCommand)).append("\n\n");

				CreateTableCommand createTableCommand = new CreateTableCommand(_buildTableDefinition(x));
				createTableCommand.getTableDefinition().setTableName(MicroOssUtils
						.getTableNameWithPrefix(createTableCommand.getTableDefinition().getTableName(), tablePrefix));
				sb.append(MysqlUtils.buildSql(createTableCommand)).append("\n\n");
			}

			for (PlainEntityRefDefinition x : modelDefinition.getEntityRefDefinitions()) {
				Class<?> sourceClz = getEntityClass(x.getSource().getName(), javaPackageName);
				Class<?> targetClz = getEntityClass(x.getTarget().getName(), javaPackageName);
				sb.append(String.format("# Dump of ref table %s and %s\n", sourceClz.getSimpleName(),
						targetClz.getSimpleName()));
				sb.append("# ------------------------------------------------------------\n\n");

				DropTableCommand dropTableCommand = MicroRefsUtils.buildDropTableCommand(sourceClz, targetClz);
				dropTableCommand.setTableName(
						MicroOssUtils.getTableNameWithPrefix(dropTableCommand.getTableName(), tablePrefix));
				sb.append(MysqlUtils.buildSql(dropTableCommand)).append("\n\n");

				CreateTableCommand createTableCommand = MicroRefsUtils.buildCreateTableCommand(sourceClz, targetClz);
				createTableCommand.getTableDefinition().setTableName(MicroOssUtils
						.getTableNameWithPrefix(createTableCommand.getTableDefinition().getTableName(), tablePrefix));
				sb.append(MysqlUtils.buildSql(createTableCommand)).append("\n\n");
			}
			return sb.toString();
		}

		/**
		 * 构建sql mock脚本
		 * 
		 * @param modelDefinition
		 * @param excludeModelNames
		 * @param tablePrefix
		 * @param javaPackageName
		 * @return
		 * @throws Exception
		 */
		public static String buildMockSql(PlainModelDefinition modelDefinition, List<String> excludeModelNames,
				String tablePrefix, String javaPackageName) throws Exception {
			StringBuffer sb = new StringBuffer();
			PlainModelMock modelMock = MockUtils.mock(modelDefinition, excludeModelNames, javaPackageName);
			for (PlainEntityMock x : modelMock.getEntityMocks()) {
				sb.append(String.format("# Mock of table %s\n", x.getClz().getSimpleName()));
				sb.append("# ------------------------------------------------------------\n\n");

				for (Object instance : x.getInstances()) {
					InsertCommand insertCommand = new InsertCommand(MicroOssUtils.buildEntity(instance));
					insertCommand.getEntity().setTableName(MicroOssUtils
							.getTableNameWithPrefix(insertCommand.getEntity().getTableName(), tablePrefix));
					sb.append(MysqlUtils.buildSql(insertCommand)).append("\n\n");
				}
			}

			for (PlainEntityRefMock x : modelMock.getEntityRefMocks()) {
				sb.append(String.format("# Mock of ref table %s and %s\n", x.getSourceClz().getSimpleName(),
						x.getTargetClz().getSimpleName()));
				sb.append("# ------------------------------------------------------------\n\n");
				for (Ref ref : x.getRefs()) {
					InsertCommand insertCommand = MicroRefsUtils.buildInsertCommand(ref);
					insertCommand.getEntity().setTableName(MicroOssUtils
							.getTableNameWithPrefix(insertCommand.getEntity().getTableName(), tablePrefix));
					sb.append(MysqlUtils.buildSql(insertCommand)).append("\n\n");
				}
			}
			return sb.toString();
		}

		private static TableDefinition _buildTableDefinition(PlainEntityDefinition entityDefinition) {
			List<FieldDefinition> fields = new ArrayList<>();
			for (PlainFieldDefinition x : entityDefinition.getFieldDefinitions()) {
				String fieldName = x.getName();
				fields.add(new FieldDefinition(fieldName, x.getComment(), FieldTypeEnum.parse(x.getJavaType()),
						x.getDefaultValue(), x.getPrimaryKey()));
			}
			return new TableDefinition(entityDefinition.getName(), entityDefinition.getComment(), fields);
		}
	}

	/**
	 * 更新Model实体类
	 */
	public static class UpdateJavaSourceFile {

		public static void updateAll(PlainModelDefinition modelDefinition, String srcFolder, String javaPackageName)
				throws Exception {
			updateAll(modelDefinition, srcFolder, javaPackageName, null);
		}

		/**
		 * 覆盖更新全部Model实体类
		 * 
		 * @param modelDefinition
		 * @param srcFolder
		 * @param javaPackageName
		 * @param javaImports
		 * @throws Exception
		 */
		public static void updateAll(PlainModelDefinition modelDefinition, String srcFolder, String javaPackageName,
				List<String> javaImports) throws Exception {
			for (PlainEntityDefinition x : modelDefinition.getEntityDefinitions()) {
				update(x, srcFolder, javaPackageName, javaImports);
			}
		}

		public static void update(PlainEntityDefinition entityDefinition, String srcFolder, String javaPackageName)
				throws IOException {
			update(entityDefinition, srcFolder, javaPackageName, null);
		}

		/**
		 * 覆盖更新单个Model实体类
		 * 
		 * @param entityDefinition
		 * @param srcFolder
		 * @param javaPackageName
		 * @param javaImports
		 * @throws IOException
		 */
		public static void update(PlainEntityDefinition entityDefinition, String srcFolder, String javaPackageName,
				List<String> javaImports) throws IOException {
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
			if (javaImports != null) {
				for (String javaImport : javaImports) {
					cu.addImport(javaImport);
				}
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
			EnumFieldDefinition enumFieldDefinition = parseEnumField(fieldDefinition);
			if (null == enumFieldDefinition) {
				return;
			}

			EnumDeclaration enumDeclaration = new EnumDeclaration(EnumSet.of(Modifier.PUBLIC),
					String.format(enum_field_class_name_format, StringUtils.capitalize(fieldDefinition.getName())));
			classDeclaration.addMember(enumDeclaration);
			for (Pair x : enumFieldDefinition.getPairs()) {
				enumDeclaration.addEnumConstant(x.getName()).setJavadocComment(x.getComment());
			}
		}

		public static EnumFieldDefinition parseEnumField(PlainFieldDefinition fieldDefinition) {
			return _parseEnumField(fieldDefinition.getComment());
		}

		private static EnumFieldDefinition _parseEnumField(String fieldComment) {
			Pattern pattern = Pattern.compile("[(（]([A-Za-z0-9_]+[:：][^,，:：)）]+[,，]?)+[)）]");
			Matcher matcher = pattern.matcher(fieldComment);
			if (!matcher.find()) {
				return null;
			}
			String enumDefinition = matcher.group();
			String[] pairs = enumDefinition.substring(1, enumDefinition.length() - 1).split("[,，]");
			EnumFieldDefinition enumFieldDefinition = new EnumFieldDefinition();
			for (String pair : pairs) {
				if (StringUtils.isBlank(pair)) {
					continue;
				}
				String[] kv = pair.split("[:：]");
				enumFieldDefinition.getPairs().add(new Pair(kv[0], kv[1]));
			}
			return enumFieldDefinition;
		}

		public static class EnumFieldDefinition {
			private List<Pair> pairs = new ArrayList<>();

			public List<Pair> getPairs() {
				return pairs;
			}

			public void setPairs(List<Pair> pairs) {
				this.pairs = pairs;
			}

			public static class Pair {
				private String name;
				private String comment;

				public Pair(String name, String comment) {
					this.name = name;
					this.comment = comment;
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
			}
		}
	}
}
