package com.github.microprograms.micro_api_sdk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.microprograms.micro_api_runtime.annotation.MicroApi;
import com.github.microprograms.micro_api_runtime.model.Request;
import com.github.microprograms.micro_api_runtime.model.Response;
import com.github.microprograms.micro_api_runtime.model.ResponseCode;
import com.github.microprograms.micro_api_sdk.callback.Callback;
import com.github.microprograms.micro_api_sdk.callback.DefaultCallback;
import com.github.microprograms.micro_api_sdk.model.ApiDefinition;
import com.github.microprograms.micro_api_sdk.model.ApiServerDefinition;
import com.github.microprograms.micro_api_sdk.model.ErrorCodeDefinition;
import com.github.microprograms.micro_api_sdk.model.MixinDefinition;
import com.github.microprograms.micro_nested_data_model_sdk.MicroNestedDataModelSdk;
import com.github.microprograms.micro_relational_data_model_sdk.MicroRelationalDataModelSdk;
import com.github.microprograms.micro_relational_data_model_sdk.model.PlainModelerDefinition;
import com.github.microprograms.micro_relational_data_model_sdk.utils.JavaParserUtils;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.matchprocessor.ClassAnnotationMatchProcessor;

public class MicroApiSdk {
	private static final Charset encoding = Charset.forName("utf8");
	public static final String error_code_enum_class_name = "ErrorCodeEnum";
	private static Callback callback = new DefaultCallback();

	public static Callback getCallback() {
		return callback;
	}

	public static void setCallback(Callback callback) {
		MicroApiSdk.callback = callback;
	}

	public static ApiServerDefinition buildApiServerDefinition(String apiServerConfigFilePath) throws IOException {
		String json = readFile(apiServerConfigFilePath);
		return parseMixin(JSON.parseObject(json, ApiServerDefinition.class));
	}

	private static ApiServerDefinition parseMixin(ApiServerDefinition apiServerDefinition) throws IOException {
		JSONObject root = (JSONObject) JSON.toJSON(apiServerDefinition);
		List<MixinDefinition> mixinDefinitions = apiServerDefinition.getMixinDefinitions();
		if (mixinDefinitions != null && !mixinDefinitions.isEmpty()) {
			for (MixinDefinition x : mixinDefinitions) {
				String sourceString = x.getSource();
				String sourceFilePath = sourceString.substring(0, sourceString.lastIndexOf("#"));
				String sourceLocation = sourceString.substring(sourceString.lastIndexOf("#") + 1);
				JSONObject sourceJson = JSON.parseObject(readFile(sourceFilePath));
				Object source = getObjectByLocation(sourceJson, sourceLocation);
				String targetLocation = x.getTarget();
				Object target = getObjectByLocation(root, targetLocation);
				mixin(source, target);
			}
		}
		return JSON.toJavaObject(root, ApiServerDefinition.class);
	}

	public static String readFile(String file) throws IOException {
		FileInputStream input = null;
		try {
			input = new FileInputStream(file);
			List<String> lines = IOUtils.readLines(input, encoding);
			return StringUtils.join(lines, "");
		} catch (IOException e) {
			throw e;
		} finally {
			IOUtils.closeQuietly(input);
		}
	}

	private static Object getObjectByLocation(JSONObject jsonObject, String location) {
		if (StringUtils.isBlank(location)) {
			return jsonObject;
		}
		int indexOfDot = location.indexOf(".");
		if (indexOfDot == -1) {
			return jsonObject.get(location);
		}
		String key = location.substring(0, indexOfDot);
		String remainingKey = location.substring(indexOfDot + 1);
		return getObjectByLocation(jsonObject.getJSONObject(key), remainingKey);
	}

	private static void mixin(Object source, Object target) {
		if (target instanceof JSONObject) {
			JSONObject jsonObject = (JSONObject) target;
			jsonObject.putAll((JSONObject) source);
		}
		if (target instanceof JSONArray) {
			JSONArray jsonArray = (JSONArray) target;
			jsonArray.addAll((JSONArray) source);
		}
	}

	public static void deletePlainEntityJavaSourceFiles(String srcFolder, ApiServerDefinition apiServerDefinition)
			throws IOException {
		MicroRelationalDataModelSdk.deleteJavaSourceFiles(srcFolder, apiServerDefinition.getJavaPackageName());
	}

	public static void updatePlainEntityJavaSourceFiles(String srcFolder, ApiServerDefinition apiServerDefinition)
			throws IOException {
		MicroRelationalDataModelSdk.updateJavaSourceFiles(srcFolder, buildPlainModelerDefinition(apiServerDefinition));
	}

	private static PlainModelerDefinition buildPlainModelerDefinition(ApiServerDefinition apiServerDefinition) {
		PlainModelerDefinition plainModelerDefinition = new PlainModelerDefinition();
		plainModelerDefinition.setVersion(apiServerDefinition.getVersion());
		plainModelerDefinition.setJavaPackageName(apiServerDefinition.getJavaPackageName());
		plainModelerDefinition.setEntityDefinitions(apiServerDefinition.getModelDefinitions());
		return plainModelerDefinition;
	}

	public static void deleteUnusedApiJavaSourceFiles(String srcFolder, ApiServerDefinition apiServerDefinition)
			throws IOException {
		new FastClasspathScanner(apiServerDefinition.getJavaPackageName())
				.matchClassesWithAnnotation(MicroApi.class, new ClassAnnotationMatchProcessor() {
					@Override
					public void processMatch(Class<?> classWithAnnotation) {
						String javaClassName = classWithAnnotation.getSimpleName();
						if (getApiDefinitionByJavaClassName(javaClassName, apiServerDefinition) == null) {
							JavaParserUtils.buildJavaSourceFile(srcFolder, apiServerDefinition.getJavaPackageName(),
									javaClassName).delete();
						}
					}
				}).scan();
	}

	private static ApiDefinition getApiDefinitionByJavaClassName(String javaClassName,
			ApiServerDefinition apiServerDefinition) {
		for (ApiDefinition apiDefinition : apiServerDefinition.getApiDefinitions()) {
			if (javaClassName.equals(apiDefinition.getName())) {
				return apiDefinition;
			}
		}
		return null;
	}

	public static void updateApiJavaSourceFiles(String srcFolder, ApiServerDefinition apiServerDefinition)
			throws IOException {
		for (ApiDefinition apiDefinition : apiServerDefinition.getApiDefinitions()) {
			updateApiJavaSourceFile(srcFolder, apiDefinition, apiServerDefinition);
		}
	}

	private static void updateApiJavaSourceFile(String srcFolder, ApiDefinition apiDefinition,
			ApiServerDefinition apiServerDefinition) throws IOException {
		String javaPackageName = apiServerDefinition.getJavaPackageName();
		File javaFile = JavaParserUtils.buildJavaSourceFile(srcFolder, javaPackageName, apiDefinition.getName());
		CompilationUnit cu = null;
		if (javaFile.exists()) {
			cu = JavaParser.parse(javaFile, encoding);
			ClassOrInterfaceDeclaration apiClassDeclaration = _getApiClassDeclaration(cu);
			_deleteMicroApiAnnotation(apiClassDeclaration);
			_fillMicroApiAnnotation(apiClassDeclaration, apiDefinition, apiServerDefinition);
			callback.updateCoreMethodDeclaration(apiClassDeclaration, apiDefinition, cu);
			callback.updateExecuteMethodDeclaration(apiClassDeclaration, apiDefinition, cu);
			_deleteRequestAndResponseClassDeclaration(apiClassDeclaration);
			_fillReqAndRespInnerClassDeclaration(apiClassDeclaration, apiDefinition);
		} else {
			javaFile.getParentFile().mkdirs();
			javaFile.createNewFile();
			cu = new CompilationUnit(javaPackageName);
			ClassOrInterfaceDeclaration apiClassDeclaration = cu.addClass(apiDefinition.getName(), Modifier.PUBLIC);
			_fillMicroApiAnnotation(apiClassDeclaration, apiDefinition, apiServerDefinition);
			callback.updateCoreMethodDeclaration(apiClassDeclaration, apiDefinition, cu);
			callback.updateExecuteMethodDeclaration(apiClassDeclaration, apiDefinition, cu);
			_fillReqAndRespInnerClassDeclaration(apiClassDeclaration, apiDefinition);
		}
		OutputStream output = new FileOutputStream(javaFile);
		IOUtils.write(cu.toString(), output, encoding);
		IOUtils.closeQuietly(output);
	}

	private static ClassOrInterfaceDeclaration _getApiClassDeclaration(CompilationUnit cu) {
		for (ClassOrInterfaceDeclaration x : cu.getChildNodesByType(ClassOrInterfaceDeclaration.class)) {
			if (x.isAnnotationPresent(MicroApi.class)) {
				return x;
			}
		}
		return null;
	}

	private static void _deleteMicroApiAnnotation(TypeDeclaration<?> typeDeclaration) {
		Optional<AnnotationExpr> optional = typeDeclaration.getAnnotationByClass(MicroApi.class);
		if (optional.isPresent()) {
			typeDeclaration.remove(optional.get());
		}
	}

	private static void _fillMicroApiAnnotation(TypeDeclaration<?> typeDeclaration, ApiDefinition apiDefinition,
			ApiServerDefinition apiServerDefinition) {
		NormalAnnotationExpr x = typeDeclaration.addAndGetAnnotation(MicroApi.class);
		if (StringUtils.isNotBlank(apiDefinition.getComment())) {
			x.addPair("comment", "\"" + apiDefinition.getComment() + "\"");
		}
		if (StringUtils.isNotBlank(apiDefinition.getDescription())) {
			x.addPair("description", "\"" + apiDefinition.getDescription() + "\"");
		}
		if (StringUtils.isNotBlank(apiServerDefinition.getVersion())) {
			x.addPair("version", "\"" + apiServerDefinition.getVersion() + "\"");
		}
	}

	private static void _deleteRequestAndResponseClassDeclaration(ClassOrInterfaceDeclaration apiClassDeclaration) {
		for (ClassOrInterfaceDeclaration x : apiClassDeclaration
				.getChildNodesByType(ClassOrInterfaceDeclaration.class)) {
			if (x.getExtendedTypes().contains(new ClassOrInterfaceType("Request"))
					|| x.getExtendedTypes().contains(new ClassOrInterfaceType("Response"))) {
				apiClassDeclaration.remove(x);
			}
		}
	}

	private static void _fillReqAndRespInnerClassDeclaration(ClassOrInterfaceDeclaration apiClassDeclaration,
			ApiDefinition apiDefinition) {
		if (apiDefinition.getRequestDefinition() != null) {
			ClassOrInterfaceDeclaration reqInnerClassDeclaration = new ClassOrInterfaceDeclaration();
			reqInnerClassDeclaration.addModifier(Modifier.PUBLIC, Modifier.STATIC).setName("Req")
					.addExtendedType(Request.class);
			apiClassDeclaration.addMember(reqInnerClassDeclaration);
			MicroNestedDataModelSdk.fillFields(reqInnerClassDeclaration, apiDefinition.getRequestDefinition());
		}
		if (apiDefinition.getResponseDefinition() != null) {
			ClassOrInterfaceDeclaration respInnerClassDeclaration = new ClassOrInterfaceDeclaration();
			respInnerClassDeclaration.addModifier(Modifier.PUBLIC, Modifier.STATIC).setName("Resp")
					.addExtendedType(Response.class);
			apiClassDeclaration.addMember(respInnerClassDeclaration);
			MicroNestedDataModelSdk.fillFields(respInnerClassDeclaration, apiDefinition.getResponseDefinition());
		}
	}

	public static void updateErrorCodeJavaFile(String srcFolder, ApiServerDefinition apiServerDefinition)
			throws IOException {
		String javaPackageName = apiServerDefinition.getJavaPackageName();
		File javaFile = JavaParserUtils.buildJavaSourceFile(srcFolder, javaPackageName, error_code_enum_class_name);
		CompilationUnit cu = null;
		if (javaFile.exists()) {
			cu = JavaParser.parse(javaFile, encoding);
			EnumDeclaration errorCodeEnumDeclaration = cu.getEnumByName(error_code_enum_class_name).get();
			_deleteErrorCodeEnumDeclaration(errorCodeEnumDeclaration);
			_fillErrorCodeEnumDeclaration(errorCodeEnumDeclaration, apiServerDefinition);
		} else {
			javaFile.getParentFile().mkdirs();
			javaFile.createNewFile();
			cu = new CompilationUnit(javaPackageName);
			EnumDeclaration errorCodeEnumDeclaration = cu.addEnum(error_code_enum_class_name, Modifier.PUBLIC);
			errorCodeEnumDeclaration.addImplementedType(ResponseCode.class);
			BlockStmt constructorBody = new BlockStmt();
			constructorBody.addStatement(
					new AssignExpr(new FieldAccessExpr(new ThisExpr(), "code"), new NameExpr("code"), Operator.ASSIGN));
			constructorBody.addStatement(new AssignExpr(new FieldAccessExpr(new ThisExpr(), "message"),
					new NameExpr("message"), Operator.ASSIGN));
			errorCodeEnumDeclaration.addConstructor(Modifier.PRIVATE).addParameter(PrimitiveType.intType(), "code")
					.addParameter(String.class, "message").setBody(constructorBody);
			errorCodeEnumDeclaration.addField(PrimitiveType.intType(), "code", Modifier.PRIVATE, Modifier.FINAL)
					.createGetter();
			errorCodeEnumDeclaration.addField(String.class, "message", Modifier.PRIVATE, Modifier.FINAL).createGetter();
			_fillErrorCodeEnumDeclaration(errorCodeEnumDeclaration, apiServerDefinition);
		}
		OutputStream output = new FileOutputStream(javaFile);
		IOUtils.write(cu.toString(), output, encoding);
		IOUtils.closeQuietly(output);
	}

	private static void _deleteErrorCodeEnumDeclaration(EnumDeclaration errorCodeEnumDeclaration) {
		for (Object enumConstantDeclaration : errorCodeEnumDeclaration.getEntries().toArray()) {
			errorCodeEnumDeclaration.remove((Node) enumConstantDeclaration);
		}
	}

	private static void _fillErrorCodeEnumDeclaration(EnumDeclaration errorCodeEnumDeclaration,
			ApiServerDefinition apiServerDefinition) {
		for (ErrorCodeDefinition errorCodeDefinition : apiServerDefinition.getErrorCodeDefinitions()) {
			errorCodeEnumDeclaration.addEnumConstant(errorCodeDefinition.getName())
					.setJavadocComment(errorCodeDefinition.getMessage())
					.addArgument(new IntegerLiteralExpr(errorCodeDefinition.getCode()))
					.addArgument(new StringLiteralExpr(errorCodeDefinition.getMessage()));
		}
	}
}
