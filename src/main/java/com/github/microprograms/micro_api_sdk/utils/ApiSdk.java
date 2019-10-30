package com.github.microprograms.micro_api_sdk.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.microprograms.micro_api_runtime.enums.ReserveResponseCodeEnum;
import com.github.microprograms.micro_api_runtime.exception.PassthroughException;
import com.github.microprograms.micro_api_runtime.model.Api;
import com.github.microprograms.micro_api_runtime.model.Request;
import com.github.microprograms.micro_api_runtime.model.Response;
import com.github.microprograms.micro_api_runtime.model.ResponseCode;
import com.github.microprograms.micro_api_sdk.model.ApiDefinition;
import com.github.microprograms.micro_api_sdk.model.ChangeLog;
import com.github.microprograms.micro_api_sdk.model.ErrorCodeDefinition;
import com.github.microprograms.micro_api_sdk.model.MixinDefinition;
import com.github.microprograms.micro_api_sdk.model.ModuleDefinition;
import com.github.microprograms.micro_api_sdk.model.ServerAddressDefinition;
import com.github.microprograms.micro_api_sdk.model.ShowdocDefinition;
import com.github.microprograms.micro_model_sdk.MicroModelSdk;
import com.github.microprograms.micro_model_sdk.model.PlainEntityDefinition;
import com.github.microprograms.micro_model_sdk.model.PlainFieldDefinition;
import com.jcabi.http.request.JdkRequest;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.matchprocessor.ImplementingClassMatchProcessor;

public class ApiSdk {
	private static final Charset encoding = Charset.forName("utf8");

	public static ModuleDefinition build(String configFilePath) throws IOException {
		String json = Fn.readFile(configFilePath, encoding);
		return _parseMixin(JSON.parseObject(json, ModuleDefinition.class));
	}

	private static ModuleDefinition _parseMixin(ModuleDefinition moduleDefinition) throws IOException {
		JSONObject root = (JSONObject) JSON.toJSON(moduleDefinition);
		List<MixinDefinition> mixinDefinitions = moduleDefinition.getMixinDefinitions();
		if (mixinDefinitions != null && !mixinDefinitions.isEmpty()) {
			for (MixinDefinition x : mixinDefinitions) {
				String sourceString = x.getSource();
				String sourceFilePath = sourceString.substring(0, sourceString.lastIndexOf("#"));
				String sourceLocation = sourceString.substring(sourceString.lastIndexOf("#") + 1);
				JSONObject sourceJson = JSON.parseObject(Fn.readFile(sourceFilePath, encoding));
				Object source = _getObjectByLocation(sourceJson, sourceLocation);
				String targetLocation = x.getTarget();
				Object target = _getObjectByLocation(root, targetLocation);
				_mixin(source, target);
			}
		}
		return JSON.toJavaObject(root, ModuleDefinition.class);
	}

	private static Object _getObjectByLocation(JSONObject jsonObject, String location) {
		if (StringUtils.isBlank(location)) {
			return jsonObject;
		}
		int indexOfDot = location.indexOf(".");
		if (indexOfDot == -1) {
			return jsonObject.get(location);
		}
		String key = location.substring(0, indexOfDot);
		String remainingKey = location.substring(indexOfDot + 1);
		return _getObjectByLocation(jsonObject.getJSONObject(key), remainingKey);
	}

	private static void _mixin(Object source, Object target) {
		if (target instanceof JSONObject) {
			JSONObject jsonObject = (JSONObject) target;
			jsonObject.putAll((JSONObject) source);
		}
		if (target instanceof JSONArray) {
			JSONArray jsonArray = (JSONArray) target;
			jsonArray.addAll((JSONArray) source);
		}
	}

	/**
	 * 接口文档
	 */
	public static class Markdown {

		public static void writeToFile(ModuleDefinition moduleDefinition, List<String> excludeApiNames, File dir)
				throws Exception {
			String data = new SimpleDateFormat("yyyyMMdd").format(new Date());
			String ver = moduleDefinition.getVersion().replaceFirst("^v", "");
			String filename = String.format("api-v%s-%s.md", ver, data);
			List<String> list = new ArrayList<>();
			list.add(buildMarkdownForHomePage(moduleDefinition));
			list.add(buildMarkdownForErrorCode(moduleDefinition));
			list.add(buildMarkdownForEntityDefinition(moduleDefinition));
			list.add(buildMarkdownForApis(moduleDefinition, excludeApiNames));
			FileUtils.writeStringToFile(new File(dir, filename), StringUtils.join(list, ""), "utf8");
		}

		private static String _buildMarkdownForChangeLogs(ModuleDefinition moduleDefinition) {
			StringBuffer sb = new StringBuffer();
			sb.append("**ChangeLog**").append("\n\n");
			for (ChangeLog log : moduleDefinition.getChangeLogs()) {
				sb.append(log.getVersion()).append("\n\n");
				for (String item : log.getItems()) {
					sb.append("* ").append(item).append("\n\n");
				}
			}
			return sb.toString();
		}

		public static String buildMarkdownForHomePage(ModuleDefinition moduleDefinition) {
			StringBuffer sb = new StringBuffer();
			sb.append("# ").append(moduleDefinition.getComment()).append("\n\n");
			sb.append("**Version**").append("\n\n");
			sb.append(String.format("`%s %s`", moduleDefinition.getVersion(), _getTime())).append("\n\n");
			sb.append(_buildMarkdownForChangeLogs(moduleDefinition));
			sb.append("## 总体说明").append("\n\n");
			sb.append("### 接口规则").append("\n\n");
			sb.append("* 传输方式").append("\n\n");
			sb.append("  HTTP").append("\n\n");
			sb.append("* 提交方式").append("\n\n");
			sb.append("  POST").append("\n\n");
			sb.append("* 请求头设置").append("\n\n");
			sb.append("  Content-Type=application/json;charset=UTF-8").append("\n\n");
			sb.append("* 数据格式").append("\n\n");
			sb.append("  JSON").append("\n\n");
			sb.append("### 数据类型").append("\n\n");
			sb.append("* string").append("\n\n");
			sb.append("  字符串（可包含数字、字母、中文和可见符号）").append("\n\n");
			sb.append("* number").append("\n\n");
			sb.append("  数值（只能是数字）").append("\n\n");
			sb.append("* object").append("\n\n");
			sb.append("  对象").append("\n\n");
			sb.append("* array").append("\n\n");
			sb.append("  数组").append("\n\n");
			sb.append("* bool").append("\n\n");
			sb.append("  布尔（true or false）").append("\n\n");
			return sb.toString();
		}

		private static String _getTime() {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		}

		public static String buildMarkdownForErrorCode(ModuleDefinition moduleDefinition) {
			StringBuffer sb = new StringBuffer();
			sb.append("## ").append("全局错误码").append("\n");
			sb.append("|错误码|错误解释|").append("\n");
			sb.append("|-----|-----|").append("\n");
			for (ErrorCodeDefinition x : moduleDefinition.getErrorCodeDefinitions()) {
				sb.append("|").append(x.getCode()).append("|").append(x.getMessage()).append("|").append("\n");
			}
			return sb.toString();
		}

		public static String buildMarkdownForEntityDefinition(ModuleDefinition moduleDefinition) {
			StringBuffer sb = new StringBuffer();
			sb.append("## ").append("实体定义").append("\n");
			for (int i = 0; i < moduleDefinition.getModelDefinitions().size(); i++) {
				PlainEntityDefinition entityDefinition = moduleDefinition.getModelDefinitions().get(i);
				sb.append("### ").append(
						String.format("%s（%s）", entityDefinition.getComment(), entityDefinition.getJavaClassName()))
						.append("\n\n");
				sb.append("|字段名|类型|长度|说明|").append("\n");
				sb.append("|-----|-----|-----|-----|").append("\n");
				for (PlainFieldDefinition x : entityDefinition.getFieldDefinitions()) {
					int length = _getLength(x);
					sb.append("|").append(x.getName()).append("|").append(_getType(x.getJavaType())).append("|")
							.append(length == 0 ? "-" : length).append("|").append(x.getComment()).append("|")
							.append("\n");
				}
			}
			String markdown = sb.toString();
			return StringUtils.isBlank(markdown) ? "无" : markdown;
		}

		private static int _getLength(PlainFieldDefinition fieldDefinition) {
			if (fieldDefinition.getExt() == null || !fieldDefinition.getExt().containsKey("length")) {
				return 0;
			}
			return (int) fieldDefinition.getExt().get("length");
		}

		private static String _getType(String javaType) {
			switch (javaType) {
			case "Boolean":
				return "bool";
			case "Integer":
				return "int";
			case "Long":
				return "long";
			case "String":
				return "string";
			default:
				return javaType.replaceFirst(".*\\.", "").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
			}
		}

		public static String buildMarkdownForApis(ModuleDefinition moduleDefinition, List<String> excludeApiNames)
				throws ClassNotFoundException, IOException {
			StringBuffer sb = new StringBuffer();
			sb.append("## ").append("接口说明").append("\n\n");
			for (ApiDefinition x : moduleDefinition.getApiDefinitions()) {
				if (excludeApiNames.contains(x.getName())) {
					continue;
				}
				sb.append(buildMarkdownForApi(x, moduleDefinition));
			}
			return sb.toString();
		}

		public static String buildMarkdownForApi(ApiDefinition apiDefinition, ModuleDefinition moduleDefinition)
				throws ClassNotFoundException, IOException {
			StringBuffer sb = new StringBuffer();
			sb.append("### ").append(apiDefinition.getComment()).append("\n\n");
			if (StringUtils.isNotBlank(apiDefinition.getDescription())) {
				sb.append("**描述：**").append("\n\n");
				sb.append("- ").append(apiDefinition.getDescription()).append("\n\n");
			}
			sb.append("**请求地址：**").append("\n\n");
			sb.append(_buildApiUrl(apiDefinition, moduleDefinition)).append("\n\n");

			sb.append("**请求参数：**").append("\n\n");
			sb.append("|参数名|必选|类型|说明|").append("\n");
			sb.append("|-----|-----|-----|-----|").append("\n");
			_appendCommonRequestFields(apiDefinition);
			for (PlainFieldDefinition x : apiDefinition.getRequestDefinition().getFieldDefinitions()) {
				String filedName = Fn.databaseIdentifierSplitCase(x.getName());
				sb.append("|").append(filedName).append("|").append(x.getRequired() ? "是" : "否").append("|")
						.append(_getType(x.getJavaType())).append("|").append(x.getComment()).append("|").append("\n");
			}
			sb.append("**请求参数示例**").append("\n\n");
			sb.append("```").append("\n");
			sb.append(JsonPrettyPrinter
					.format(_buildRequestExampleInJson(apiDefinition.getRequestDefinition()).toJSONString()))
					.append("\n");
			sb.append("```").append("\n\n");

			sb.append("**返回参数**").append("\n\n");
			sb.append("|参数名|类型|说明|").append("\n");
			sb.append("|-----|-----|-----|").append("\n");
			_appendCommonResponseFields(apiDefinition, moduleDefinition);
			for (PlainFieldDefinition x : apiDefinition.getResponseDefinition().getFieldDefinitions()) {
				String filedName = Fn.databaseIdentifierSplitCase(x.getName());
				sb.append("|").append(filedName).append("|").append(_getType(x.getJavaType())).append("|")
						.append(x.getComment()).append("|").append("\n");
			}
			sb.append("**返回参数示例**").append("\n\n");
			sb.append("```").append("\n");
			sb.append(JsonPrettyPrinter
					.format(_buildResponseExampleInJson(apiDefinition.getResponseDefinition()).toJSONString()))
					.append("\n");
			sb.append("```").append("\n\n");

			return sb.toString();
		}

		private static String _buildApiUrl(ApiDefinition apiDefinition, ModuleDefinition moduleDefinition) {
			StringBuilder sb = new StringBuilder();
			ServerAddressDefinition serverAddressDefinition = moduleDefinition.getServerAddressDefinition();
			sb.append(String.format("http://%s:%s", serverAddressDefinition.getHost(),
					serverAddressDefinition.getPort()));
			sb.append(moduleDefinition.getServerAddressDefinition().getUrl());
			Map<String, Object> ext = apiDefinition.getExt();
			if (ext != null) {
				sb.append((String) ext.getOrDefault("url", ""));
			}
			return sb.toString();
		}

		private static void _appendCommonRequestFields(ApiDefinition apiDefinition) {
			if (apiDefinition.getRequestDefinition() == null) {
				apiDefinition.setRequestDefinition(new PlainEntityDefinition());
			}
			if (apiDefinition.getRequestDefinition().getFieldDefinitions() == null) {
				apiDefinition.getRequestDefinition().setFieldDefinitions(new ArrayList<>());
			}
			List<PlainFieldDefinition> fields = new ArrayList<>();
			fields.add(_buildField("接口名", "apiName", "String", true, apiDefinition.getName()));
			apiDefinition.getRequestDefinition().getFieldDefinitions().addAll(0, fields);
		}

		private static void _appendCommonResponseFields(ApiDefinition apiDefinition, ModuleDefinition moduleDefinition)
				throws ClassNotFoundException, IOException {
			if (apiDefinition.getResponseDefinition() == null) {
				apiDefinition.setResponseDefinition(new PlainEntityDefinition());
			}
			if (apiDefinition.getResponseDefinition().getFieldDefinitions() == null) {
				apiDefinition.getResponseDefinition().setFieldDefinitions(new ArrayList<>());
			}
			List<PlainFieldDefinition> fields = new ArrayList<>();
			fields.add(_buildField("错误码（success表示成功，其他的都表示错误）", "code", "String", true,
					ReserveResponseCodeEnum.success.getCode()));
			fields.add(_buildField("错误提示", "message", "String", true, ReserveResponseCodeEnum.success.getMessage()));
			apiDefinition.getResponseDefinition().getFieldDefinitions().addAll(0, fields);
		}

		private static PlainFieldDefinition _buildField(String comment, String name, String javaType, boolean required,
				Object example) {
			PlainFieldDefinition fieldDefinition = new PlainFieldDefinition();
			fieldDefinition.setComment(comment);
			fieldDefinition.setJavaType(javaType);
			fieldDefinition.setName(name);
			fieldDefinition.setRequired(required);
			fieldDefinition.setExample(example);
			return fieldDefinition;
		}

		private static JSONObject _buildRequestExampleInJson(PlainEntityDefinition entityDefinition) {
			JSONObject json = new JSONObject();
			for (PlainFieldDefinition fieldDefinition : entityDefinition.getFieldDefinitions()) {
				json.put(Fn.databaseIdentifierSplitCase(fieldDefinition.getName()), fieldDefinition.getExample());
			}
			return json;
		}

		private static JSONObject _buildResponseExampleInJson(PlainEntityDefinition entityDefinition) {
			JSONObject json = new JSONObject();
			for (PlainFieldDefinition fieldDefinition : entityDefinition.getFieldDefinitions()) {
				json.put(Fn.databaseIdentifierSplitCase(fieldDefinition.getName()), fieldDefinition.getExample());
			}
			return json;
		}
	}

	/**
	 * 在线版接口文档
	 */
	public static class ShowDoc {

		public static void update(ModuleDefinition moduleDefinition) throws Exception {
			_updateErrorCodePage(moduleDefinition);
			_updateEntityDefinitionPage(moduleDefinition);
			_updateApiPages(moduleDefinition);
			_updateHomePage(moduleDefinition);
		}

		private static void _updatePage(String catName, String pageTitle, String pageContent, int sNumber,
				ShowdocDefinition showdocDefinition) throws IOException {
			Map<String, String> req = new HashMap<>();
			req.put("api_key", showdocDefinition.getApiKey());
			req.put("api_token", showdocDefinition.getApiToken());
			req.put("cat_name", catName);
			req.put("cat_name_sub", "");
			req.put("page_title", pageTitle);
			req.put("page_content", pageContent);
			req.put("s_number", String.valueOf(sNumber));
			String response = new JdkRequest(showdocDefinition.getUrl()).body().formParams(req).back()
					.method(com.jcabi.http.Request.POST).fetch().body();
			JSONObject resp = JSON.parseObject(response);
			if (resp.getIntValue("error_code") != 0) {
				throw new RuntimeException(response);
			}
		}

		private static void _updateHomePage(ModuleDefinition moduleDefinition) throws IOException {
			_updatePage("", "说明", Markdown.buildMarkdownForHomePage(moduleDefinition), 1,
					moduleDefinition.getShowdocDefinition());
		}

		private static void _updateErrorCodePage(ModuleDefinition moduleDefinition) throws IOException {
			_updatePage("", "全局错误码", Markdown.buildMarkdownForErrorCode(moduleDefinition), 2,
					moduleDefinition.getShowdocDefinition());
		}

		private static void _updateEntityDefinitionPage(ModuleDefinition moduleDefinition) throws IOException {
			_updatePage("", "实体定义", Markdown.buildMarkdownForEntityDefinition(moduleDefinition), 3,
					moduleDefinition.getShowdocDefinition());
		}

		private static void _updateApiPages(ModuleDefinition moduleDefinition)
				throws IOException, ClassNotFoundException {
			List<ApiDefinition> apiDefinitions = moduleDefinition.getApiDefinitions();
			for (int i = 0; i < apiDefinitions.size(); i++) {
				ApiDefinition apiDefinition = apiDefinitions.get(i);
				String comment = apiDefinition.getComment();
				String catName = comment.indexOf('-') == -1 ? "" : comment.replaceFirst("\\s*-.*$", "");
				String pageTitle = comment.replaceFirst("^.*-\\s*", "");
				String pageContent = Markdown.buildMarkdownForApi(apiDefinition, moduleDefinition);
				_updatePage(catName, pageTitle, pageContent, 100 + i, moduleDefinition.getShowdocDefinition());
			}
		}
	}

	/**
	 * 更新API类
	 */
	public static class UpdateJavaSourceFile {
		public static final String error_code_enum_class_name = "ErrorCodeEnum";

		/**
		 * 更新策略
		 */
		public static abstract class UpdateStrategy {
			abstract void updateExecuteMethod(ClassOrInterfaceDeclaration apiClassDeclaration, CompilationUnit cu,
					ApiDefinition apiDefinition);

			abstract void updateCoreMethod(ClassOrInterfaceDeclaration apiClassDeclaration, CompilationUnit cu,
					ApiDefinition apiDefinition);

			protected void removeMethod(ClassOrInterfaceDeclaration apiClassDeclaration, String name,
					String... paramTypes) {
				for (MethodDeclaration x : apiClassDeclaration.getMethodsBySignature(name, paramTypes)) {
					x.remove();
				}
			}

			protected boolean existMethod(ClassOrInterfaceDeclaration apiClassDeclaration, String name,
					String... paramTypes) {
				return !apiClassDeclaration.getMethodsBySignature(name, paramTypes).isEmpty();
			}

			protected MethodDeclaration getMethod(ClassOrInterfaceDeclaration apiClassDeclaration, String name,
					String... paramTypes) {
				List<MethodDeclaration> list = apiClassDeclaration.getMethodsBySignature(name, paramTypes);
				return list.isEmpty() ? null : list.get(0);
			}

			protected String getRequestType(ApiDefinition apiDefinition) {
				return apiDefinition.getRequestDefinition() != null ? "Req" : "Request";
			}

			protected String getResponseType(ApiDefinition apiDefinition) {
				return apiDefinition.getResponseDefinition() != null ? "Resp" : "Response";
			}
		}

		/**
		 * 默认更新策略
		 */
		public static class DefaultUpdateStrategy extends UpdateStrategy {

			@Override
			public void updateExecuteMethod(ClassOrInterfaceDeclaration apiClassDeclaration, CompilationUnit cu,
					ApiDefinition apiDefinition) {
				removeMethod(apiClassDeclaration, "execute", "Request");
				MethodDeclaration executeMethod = apiClassDeclaration.addMethod("execute", Modifier.PUBLIC);
				executeMethod.setType(Response.class);
				executeMethod.addParameter(Request.class, "request");
				executeMethod.addThrownException(PassthroughException.class);
				executeMethod.addMarkerAnnotation(Override.class);
				BlockStmt block = new BlockStmt();
				if (apiDefinition.getRequestDefinition() != null) {
					block.addStatement(new AssignExpr(
							new VariableDeclarationExpr(new ClassOrInterfaceType("Req"), "req"),
							new CastExpr(new ClassOrInterfaceType("Req"), new NameExpr("request")), Operator.ASSIGN));
				} else {
					block.addStatement(
							new AssignExpr(new VariableDeclarationExpr(new ClassOrInterfaceType("Request"), "req"),
									new NameExpr("request"), Operator.ASSIGN));
				}
				if (apiDefinition.getResponseDefinition() != null) {
					block.addStatement(new AssignExpr(
							new VariableDeclarationExpr(new ClassOrInterfaceType("Resp"), "resp"),
							new ObjectCreationExpr().setType(new ClassOrInterfaceType("Resp")), Operator.ASSIGN));
				} else {
					block.addStatement(new AssignExpr(
							new VariableDeclarationExpr(new ClassOrInterfaceType("Response"), "resp"),
							new ObjectCreationExpr().setType(new ClassOrInterfaceType("Response")), Operator.ASSIGN));
				}
				block.addStatement(new MethodCallExpr(null, new SimpleName("core"),
						NodeList.nodeList(new NameExpr("req"), new NameExpr("resp"))));
				block.addStatement(new ReturnStmt(new NameExpr("resp")));
				executeMethod.setBody(block);
			}

			@Override
			public void updateCoreMethod(ClassOrInterfaceDeclaration apiClassDeclaration, CompilationUnit cu,
					ApiDefinition apiDefinition) {
				if (existMethod(apiClassDeclaration, "core", getRequestType(apiDefinition),
						getResponseType(apiDefinition))) {
					return;
				}
				cu.addImport(ReserveResponseCodeEnum.class.getName());
				MethodDeclaration coreMethod = apiClassDeclaration.addMethod("core", Modifier.PRIVATE);
				coreMethod.addParameter(new ClassOrInterfaceType(getRequestType(apiDefinition)), "req");
				coreMethod.addParameter(new ClassOrInterfaceType(getResponseType(apiDefinition)), "resp");
				coreMethod.addThrownException(PassthroughException.class);
				BlockStmt block = new BlockStmt();
				block.addStatement(new ThrowStmt(new ObjectCreationExpr(null,
						new ClassOrInterfaceType(PassthroughException.class.getSimpleName()),
						NodeList.nodeList(
								new FieldAccessExpr(new NameExpr(ReserveResponseCodeEnum.class.getSimpleName()),
										ReserveResponseCodeEnum.api_not_implemented_exception.name())))));
				coreMethod.setBody(block);
			}
		}

		/**
		 * 删除无用的API类
		 * 
		 * @param moduleDefinition
		 * @param srcFolder
		 * @param javaPackageName
		 * @throws IOException
		 */
		public static void deleteUnusedApis(ModuleDefinition moduleDefinition, String srcFolder, String javaPackageName)
				throws IOException {
			new FastClasspathScanner(javaPackageName) //
					.matchClassesImplementing(Api.class, new ImplementingClassMatchProcessor<Api>() {
						@Override
						public void processMatch(Class<? extends Api> apiClass) {
							String apiClassName = apiClass.getSimpleName();
							if (_getApiDefinitionByJavaClassName(apiClassName, moduleDefinition) != null) {
								return;
							}
							JavaParserUtils.buildJavaSourceFile(srcFolder, javaPackageName, apiClassName).delete();
						}
					}).scan();
		}

		private static ApiDefinition _getApiDefinitionByJavaClassName(String javaClassName,
				ModuleDefinition moduleDefinition) {
			for (ApiDefinition x : moduleDefinition.getApiDefinitions()) {
				if (javaClassName.equals(x.getName())) {
					return x;
				}
			}
			return null;
		}

		/**
		 * 更新全部API类
		 * 
		 * @param moduleDefinition
		 * @param srcFolder
		 * @param javaPackageName
		 * @param updateStrategy
		 * @throws IOException
		 */
		public static void updateAllApis(ModuleDefinition moduleDefinition, String srcFolder, String javaPackageName,
				UpdateStrategy updateStrategy) throws IOException {
			for (ApiDefinition x : moduleDefinition.getApiDefinitions()) {
				updateApi(x, moduleDefinition, srcFolder, javaPackageName, updateStrategy);
			}
		}

		/**
		 * 更新API类
		 * 
		 * @param apiDefinition
		 * @param moduleDefinition
		 * @param srcFolder
		 * @param javaPackageName
		 * @param updateStrategy
		 * @throws IOException
		 */
		private static void updateApi(ApiDefinition apiDefinition, ModuleDefinition moduleDefinition, String srcFolder,
				String javaPackageName, UpdateStrategy updateStrategy) throws IOException {
			String apiClassName = _getApiClassName(apiDefinition);
			File javaFile = JavaParserUtils.buildJavaSourceFile(srcFolder, javaPackageName, apiClassName);
			CompilationUnit cu = null;
			if (javaFile.exists()) {
				cu = JavaParser.parse(javaFile, encoding);
				ClassOrInterfaceDeclaration apiClass = _getApiClass(cu);
				updateStrategy.updateCoreMethod(apiClass, cu, apiDefinition);
				updateStrategy.updateExecuteMethod(apiClass, cu, apiDefinition);
				_deleteReqAndRespInnerClass(apiClass);
				_createReqAndRespInnerClass(apiClass, cu, apiDefinition);
			} else {
				javaFile.getParentFile().mkdirs();
				javaFile.createNewFile();
				cu = new CompilationUnit(javaPackageName);
				ClassOrInterfaceDeclaration apiClass = cu.addClass(apiClassName, Modifier.PUBLIC);
				apiClass.addImplementedType(Api.class);
				updateStrategy.updateCoreMethod(apiClass, cu, apiDefinition);
				updateStrategy.updateExecuteMethod(apiClass, cu, apiDefinition);
				_createReqAndRespInnerClass(apiClass, cu, apiDefinition);
			}
			JavaParserUtils.write(cu, javaFile, encoding);
		}

		private static String _getApiClassName(ApiDefinition apiDefinition) {
			return StringUtils.capitalize(apiDefinition.getName().replace(".", "_"));
		}

		private static ClassOrInterfaceDeclaration _getApiClass(CompilationUnit cu) {
			for (ClassOrInterfaceDeclaration x : cu.getChildNodesByType(ClassOrInterfaceDeclaration.class)) {
				if (x.getImplementedTypes().contains(new ClassOrInterfaceType(Api.class.getSimpleName()))) {
					return x;
				}
			}
			return null;
		}

		private static void _deleteReqAndRespInnerClass(ClassOrInterfaceDeclaration apiClassDeclaration) {
			for (ClassOrInterfaceDeclaration x : apiClassDeclaration
					.getChildNodesByType(ClassOrInterfaceDeclaration.class)) {
				if (x.getExtendedTypes().contains(new ClassOrInterfaceType(Request.class.getSimpleName()))
						|| x.getExtendedTypes().contains(new ClassOrInterfaceType(Response.class.getSimpleName()))) {
					apiClassDeclaration.remove(x);
				}
			}
		}

		private static void _createReqAndRespInnerClass(ClassOrInterfaceDeclaration apiClassDeclaration,
				CompilationUnit cu, ApiDefinition apiDefinition) {
			cu.addImport(List.class);
			if (apiDefinition.getRequestDefinition() != null) {
				ClassOrInterfaceDeclaration reqInnerClassDeclaration = new ClassOrInterfaceDeclaration();
				reqInnerClassDeclaration.addModifier(Modifier.PUBLIC, Modifier.STATIC).setName("Req")
						.addExtendedType(Request.class);
				apiClassDeclaration.addMember(reqInnerClassDeclaration);
				MicroModelSdk.fillFields(reqInnerClassDeclaration, apiDefinition.getRequestDefinition());
			}
			if (apiDefinition.getResponseDefinition() != null) {
				ClassOrInterfaceDeclaration respInnerClassDeclaration = new ClassOrInterfaceDeclaration();
				respInnerClassDeclaration.addModifier(Modifier.PUBLIC, Modifier.STATIC).setName("Resp")
						.addExtendedType(Response.class);
				apiClassDeclaration.addMember(respInnerClassDeclaration);
				MicroModelSdk.fillFields(respInnerClassDeclaration, apiDefinition.getResponseDefinition());
			}
		}

		/**
		 * 更新错误码类
		 * 
		 * @param moduleDefinition
		 * @param srcFolder
		 * @param javaPackageName
		 * @throws IOException
		 */
		public static void updateErrorCode(ModuleDefinition moduleDefinition, String srcFolder, String javaPackageName)
				throws IOException {
			File javaFile = JavaParserUtils.buildJavaSourceFile(srcFolder, javaPackageName, error_code_enum_class_name);
			CompilationUnit cu = null;
			if (javaFile.exists()) {
				cu = JavaParser.parse(javaFile, encoding);
				EnumDeclaration errorCodeEnum = cu.getEnumByName(error_code_enum_class_name).get();
				_deleteErrorCodeEnum(errorCodeEnum);
				_fillErrorCodeEnum(errorCodeEnum, moduleDefinition);
			} else {
				javaFile.getParentFile().mkdirs();
				javaFile.createNewFile();
				cu = new CompilationUnit(javaPackageName);
				EnumDeclaration errorCodeEnumDeclaration = cu.addEnum(error_code_enum_class_name, Modifier.PUBLIC);
				errorCodeEnumDeclaration.addImplementedType(ResponseCode.class);
				BlockStmt constructorBody = new BlockStmt();
				constructorBody.addStatement(new AssignExpr(new FieldAccessExpr(new ThisExpr(), "code"),
						new NameExpr("code"), Operator.ASSIGN));
				constructorBody.addStatement(new AssignExpr(new FieldAccessExpr(new ThisExpr(), "message"),
						new NameExpr("message"), Operator.ASSIGN));
				errorCodeEnumDeclaration.addConstructor(Modifier.PRIVATE).addParameter(String.class, "code")
						.addParameter(String.class, "message").setBody(constructorBody);
				errorCodeEnumDeclaration.addField(String.class, "code", Modifier.PRIVATE, Modifier.FINAL)
						.createGetter();
				errorCodeEnumDeclaration.addField(String.class, "message", Modifier.PRIVATE, Modifier.FINAL)
						.createGetter();
				_fillErrorCodeEnum(errorCodeEnumDeclaration, moduleDefinition);
			}
			JavaParserUtils.write(cu, javaFile, encoding);
		}

		private static void _deleteErrorCodeEnum(EnumDeclaration errorCodeEnumDeclaration) {
			for (Object enumConstantDeclaration : errorCodeEnumDeclaration.getEntries().toArray()) {
				errorCodeEnumDeclaration.remove((Node) enumConstantDeclaration);
			}
		}

		private static void _fillErrorCodeEnum(EnumDeclaration errorCodeEnumDeclaration,
				ModuleDefinition moduleDefinition) {
			for (ErrorCodeDefinition errorCodeDefinition : moduleDefinition.getErrorCodeDefinitions()) {
				errorCodeEnumDeclaration.addEnumConstant(errorCodeDefinition.getCode())
						.setJavadocComment(errorCodeDefinition.getMessage())
						.addArgument(new StringLiteralExpr(errorCodeDefinition.getCode()))
						.addArgument(new StringLiteralExpr(errorCodeDefinition.getMessage()));
			}
		}
	}
}
