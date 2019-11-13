package com.github.microprograms.micro_api_sdk.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSON;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.microprograms.micro_api_sdk.model.PlainEntityDefinition;
import com.github.microprograms.micro_api_sdk.model.PlainFieldDefinition;
import com.github.microprograms.micro_api_sdk.model.PlainModelerDefinition;
import com.github.microprograms.micro_oss_core.model.FieldDefinition;
import com.github.microprograms.micro_oss_core.model.FieldDefinition.FieldTypeEnum;
import com.github.microprograms.micro_oss_core.model.TableDefinition;
import com.github.microprograms.micro_oss_core.model.ddl.CreateTableCommand;
import com.github.microprograms.micro_oss_core.model.ddl.DropTableCommand;
import com.github.microprograms.micro_oss_mysql.model.ddl.PrimaryKeyDefinition;
import com.github.microprograms.micro_oss_mysql.model.ddl.TableColumnDefinition;
import com.github.microprograms.micro_oss_mysql.model.ddl.TableElementDefinition;

/**
 * 建模（表结构定义）
 */
public class ModelSdk {
	private static final Charset encoding = Charset.forName("utf8");

	public static PlainModelerDefinition build(String configFilePath) throws IOException {
		String json = Fn.readFile(configFilePath, encoding);
		return JSON.parseObject(json, PlainModelerDefinition.class);
	}

	/**
	 * sql初始化脚本
	 */
	public static class Sql {

		/**
		 * 把sql写到文件
		 * 
		 * @param modelerDefinition
		 * @param excludeModelNames
		 * @param tablePrefix
		 * @param dir
		 * @throws Exception
		 */
		public static void writeToFile(PlainModelerDefinition modelerDefinition, List<String> excludeModelNames,
				String tablePrefix, File dir) throws Exception {
			String data = new SimpleDateFormat("yyyyMMdd").format(new Date());
			String ver = modelerDefinition.getVersion().replaceFirst("^v", "");
			String filename = String.format("init-v%s-%s.sql", ver, data);
			String sql = buildInitSql(modelerDefinition, excludeModelNames, tablePrefix);
			FileUtils.writeStringToFile(new File(dir, filename), sql, encoding);
		}

		/**
		 * 构建sql初始化脚本
		 * 
		 * @param modelerDefinition
		 * @param excludeModelNames
		 * @param tablePrefix
		 * @return
		 * @throws Exception
		 */
		public static String buildInitSql(PlainModelerDefinition modelerDefinition, List<String> excludeModelNames,
				String tablePrefix) throws Exception {
			StringBuffer sb = new StringBuffer();
			for (PlainEntityDefinition x : modelerDefinition.getEntityDefinitions()) {
				if (excludeModelNames.contains(x.getJavaClassName())) {
					continue;
				}
				sb.append(String.format("# Dump of table %s（%s）\n", x.getComment(), x.getJavaClassName()));
				sb.append("# ------------------------------------------------------------\n\n");
				sb.append(_buildDropTableSql(new DropTableCommand(_getTableName(x, tablePrefix)))).append("\n\n");
				sb.append(_buildCreateTableSql(new CreateTableCommand(_buildTableDefinition(x, tablePrefix))))
						.append("\n\n");
			}
			return sb.toString();
		}

		private static String _buildDropTableSql(DropTableCommand command) {
			return String.format("DROP TABLE IF EXISTS %s;", command.getTableName());
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

		private static String _buildCreateTableSql(CreateTableCommand command) {
			StringBuffer sb = new StringBuffer("CREATE TABLE IF NOT EXISTS ");
			TableDefinition tableDefinition = command.getTableDefinition();
			sb.append(tableDefinition.getTableName());
			sb.append("(");
			PrimaryKeyDefinition primaryKeyDefinition = new PrimaryKeyDefinition();
			List<TableElementDefinition> tableElementDefinitions = new ArrayList<>();
			for (FieldDefinition fieldDefinition : tableDefinition.getFields()) {
				boolean isPrimaryKey = fieldDefinition.getPrimaryKey() > 0;
				if (isPrimaryKey) {
					primaryKeyDefinition.getFiledNames().put(fieldDefinition.getPrimaryKey(),
							fieldDefinition.getName());
				}
				String type = TableColumnDefinition.getMysqlDataType(fieldDefinition.getType(), isPrimaryKey);
				if ("id".equals(fieldDefinition.getName())) {
					type += " AUTO_INCREMENT";
				}
				TableColumnDefinition columnDefinition = new TableColumnDefinition(fieldDefinition.getName(),
						fieldDefinition.getComment(), type);
				tableElementDefinitions.add(columnDefinition);
			}
			tableElementDefinitions.add(primaryKeyDefinition);
			sb.append(StringUtils.join(tableElementDefinitions, ","));
			String tableComment = StringUtils.isBlank(tableDefinition.getComment()) ? ""
					: tableDefinition.getComment().replaceAll("'", "''");
			sb.append(String.format(") COMMENT='%s';", tableComment));
			return sb.toString();
		}

		private static String _getTableName(PlainEntityDefinition entityDefinition, String tablePrefix) {
			return Fn.databaseIdentifierSplitCase(tablePrefix, entityDefinition.getJavaClassName());
		}

		private static String _getFieldName(PlainFieldDefinition fieldDefinition) {
			return Fn.databaseIdentifierSplitCase(fieldDefinition.getName());
		}
	}

	/**
	 * 更新Model实体类
	 */
	public static class UpdateJavaSourceFile {

		/**
		 * 覆盖更新全部Model实体类
		 * 
		 * @param modelerDefinition
		 * @param srcFolder
		 * @param javaPackageName
		 * @throws Exception
		 */
		public static void updateAll(PlainModelerDefinition modelerDefinition, String srcFolder, String javaPackageName)
				throws Exception {
			for (PlainEntityDefinition x : modelerDefinition.getEntityDefinitions()) {
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
		private static void update(PlainEntityDefinition entityDefinition, String srcFolder, String javaPackageName)
				throws IOException {
			String entityJavaClassName = entityDefinition.getJavaClassName();
			File javaFile = JavaParserUtils.buildJavaSourceFile(srcFolder, javaPackageName, entityJavaClassName);
			CompilationUnit cu = null;
			if (javaFile.exists()) {
				cu = JavaParser.parse(javaFile, encoding);
				ClassOrInterfaceDeclaration entityClassDeclaration = cu.getClassByName(entityJavaClassName).get();
				List<FieldDeclaration> fieldDeclarations = entityClassDeclaration.getFields();
				for (PlainFieldDefinition x : entityDefinition.getFieldDefinitions()) {
					if (existsInFieldDeclarations(fieldDeclarations, x.getName())) {
						continue;
					}
					fillField(entityClassDeclaration, x);
				}
				for (FieldDeclaration x : fieldDeclarations) {
					if (existsInPlainFieldDefinitions(entityDefinition.getFieldDefinitions(),
							x.getVariable(0).getNameAsString())) {
						continue;
					}
					x.remove();
				}
			} else {
				javaFile.getParentFile().mkdirs();
				javaFile.createNewFile();
				cu = new CompilationUnit(javaPackageName);
				ClassOrInterfaceDeclaration modelClassDeclaration = cu.addClass(entityDefinition.getJavaClassName(),
						Modifier.PUBLIC);
				modelClassDeclaration.setComment(new JavadocComment("\n * " + entityDefinition.getComment() + "\n"));
				fillFields(modelClassDeclaration, entityDefinition);
			}
			JavaParserUtils.write(cu, javaFile, encoding);
		}

		private static boolean existsInFieldDeclarations(List<FieldDeclaration> fieldDeclarations, String fieldName) {
			for (FieldDeclaration x : fieldDeclarations) {
				if (x.getVariable(0).getNameAsString().equals(fieldName)) {
					return true;
				}
			}
			return false;
		}

		private static boolean existsInPlainFieldDefinitions(List<PlainFieldDefinition> fieldDefinitions,
				String fieldName) {
			for (PlainFieldDefinition x : fieldDefinitions) {
				if (x.getName().equals(fieldName)) {
					return true;
				}
			}
			return false;
		}

		private static void fillFields(ClassOrInterfaceDeclaration classDeclaration,
				PlainEntityDefinition entityDefinition) {
			for (PlainFieldDefinition x : entityDefinition.getFieldDefinitions()) {
				fillField(classDeclaration, x);
			}
		}

		private static void fillField(ClassOrInterfaceDeclaration classDeclaration,
				PlainFieldDefinition fieldDefinition) {
			if (fieldDefinition.getName().equals("id") || fieldDefinition.getName().equals("createdAt")
					|| fieldDefinition.getName().equals("updatedAt")) {
				return;
			}
			String type = fieldDefinition.getJavaType();
			if (type.equals("java.sql.Date")) {
				type = "Date";
			}
			FieldDeclaration fieldDeclaration = new FieldDeclaration(EnumSet.of(Modifier.PRIVATE),
					new VariableDeclarator(new ClassOrInterfaceType(type), fieldDefinition.getName()));
			classDeclaration.addMember(fieldDeclaration);
			fieldDeclaration.setLineComment(" " + fieldDefinition.getComment());
			fieldDeclaration.createGetter();
			fieldDeclaration.createSetter();
		}
	}
}
