package com.github.microprograms.micro_api_sdk.utils;

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
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.microprograms.micro_api_runtime.annotation.MicroApiAnnotation;
import com.github.microprograms.micro_api_runtime.model.Request;
import com.github.microprograms.micro_api_runtime.model.Response;
import com.github.microprograms.micro_api_runtime.model.ResponseCode;
import com.github.microprograms.micro_api_sdk.model.ApiDefinition;
import com.github.microprograms.micro_api_sdk.model.EngineDefinition;
import com.github.microprograms.micro_api_sdk.model.ErrorCodeDefinition;
import com.github.microprograms.micro_entity_definition_runtime.annotation.Comment;
import com.github.microprograms.micro_entity_definition_runtime.annotation.Description;
import com.github.microprograms.micro_entity_definition_runtime.annotation.MicroEntityAnnotation;
import com.github.microprograms.micro_entity_definition_runtime.annotation.Required;
import com.github.microprograms.micro_entity_definition_runtime.model.EntityDefinition;
import com.github.microprograms.micro_entity_definition_runtime.model.FieldDefinition;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.matchprocessor.ClassAnnotationMatchProcessor;

public class ApiEngineGeneratorUtils {
    private static final Charset encoding = Charset.forName("utf8");
    public static final String error_code_enum_class_name = "ErrorCodeEnum";

    public static void deleteModelJavaFiles(String srcFolder, EngineDefinition engineDefinition) throws IOException {
        new FastClasspathScanner(engineDefinition.getJavaPackageName()).matchClassesWithAnnotation(MicroEntityAnnotation.class, new ClassAnnotationMatchProcessor() {

            @Override
            public void processMatch(Class<?> classWithAnnotation) {
                String javaClassName = classWithAnnotation.getSimpleName();
                new File(_buildJavaFilePath(srcFolder, engineDefinition.getJavaPackageName(), javaClassName)).delete();
            }
        }).scan();
    }

    public static void createModelJavaFiles(String srcFolder, EngineDefinition engineDefinition) throws IOException {
        for (EntityDefinition x : engineDefinition.getModelDefinitions()) {
            _createModelJavaFile(srcFolder, x, engineDefinition);
        }
    }

    private static void _createModelJavaFile(String srcFolder, EntityDefinition modelDefinition, EngineDefinition engineDefinition) throws IOException {
        String javaPackageName = engineDefinition.getJavaPackageName();
        File javaFile = new File(_buildJavaFilePath(srcFolder, javaPackageName, modelDefinition.getJavaClassName()));
        javaFile.getParentFile().mkdirs();
        javaFile.createNewFile();
        CompilationUnit cu = new CompilationUnit(javaPackageName);
        if (modelDefinition.getImports() != null) {
            for (String importJavaClass : modelDefinition.getImports()) {
                cu.addImport(importJavaClass);
            }
        }
        ClassOrInterfaceDeclaration modelClassDeclaration = cu.addClass(modelDefinition.getJavaClassName(), Modifier.PUBLIC);
        modelClassDeclaration.addAndGetAnnotation(MicroEntityAnnotation.class);
        _fillFields(modelClassDeclaration, modelDefinition);
        OutputStream output = new FileOutputStream(javaFile);
        IOUtils.write(cu.toString(), output, encoding);
        IOUtils.closeQuietly(output);
    }

    public static void deleteUnusedApiJavaFiles(String srcFolder, EngineDefinition engineDefinition) throws IOException {
        new FastClasspathScanner(engineDefinition.getJavaPackageName()).matchClassesWithAnnotation(MicroApiAnnotation.class, new ClassAnnotationMatchProcessor() {

            @Override
            public void processMatch(Class<?> classWithAnnotation) {
                String javaClassName = classWithAnnotation.getSimpleName();
                if (_getApiDefinitionByJavaClassName(javaClassName, engineDefinition) == null) {
                    new File(_buildJavaFilePath(srcFolder, engineDefinition.getJavaPackageName(), javaClassName)).delete();
                }
            }
        }).scan();
    }

    private static ApiDefinition _getApiDefinitionByJavaClassName(String javaClassName, EngineDefinition engineDefinition) {
        for (ApiDefinition apiDefinition : engineDefinition.getApiDefinitions()) {
            if (javaClassName.equals(apiDefinition.getJavaClassName())) {
                return apiDefinition;
            }
        }
        return null;
    }

    private static String _buildJavaFilePath(String srcFolder, String javaPackageName, String javaClassName) {
        return srcFolder + File.separator + javaPackageName.replaceAll("\\.", File.separator) + File.separator + javaClassName + ".java";
    }

    public static void updateApiJavaFiles(String srcFolder, EngineDefinition engineDefinition) throws IOException {
        for (ApiDefinition apiDefinition : engineDefinition.getApiDefinitions()) {
            _updateApiJavaFile(srcFolder, apiDefinition, engineDefinition);
        }
    }

    public static void updateErrorCodeJavaFile(String srcFolder, EngineDefinition engineDefinition) throws IOException {
        String javaPackageName = engineDefinition.getJavaPackageName();
        File javaFile = new File(_buildJavaFilePath(srcFolder, javaPackageName, error_code_enum_class_name));
        CompilationUnit cu = null;
        if (javaFile.exists()) {
            cu = JavaParser.parse(javaFile, encoding);
            EnumDeclaration errorCodeEnumDeclaration = cu.getEnumByName(error_code_enum_class_name).get();
            _deleteErrorCodeEnumDeclaration(errorCodeEnumDeclaration);
            _fillErrorCodeEnumDeclaration(errorCodeEnumDeclaration, engineDefinition);
        } else {
            javaFile.getParentFile().mkdirs();
            javaFile.createNewFile();
            cu = new CompilationUnit(javaPackageName);
            EnumDeclaration errorCodeEnumDeclaration = cu.addEnum(error_code_enum_class_name, Modifier.PUBLIC);
            errorCodeEnumDeclaration.addImplementedType(ResponseCode.class);
            BlockStmt constructorBody = new BlockStmt();
            constructorBody.addStatement(new AssignExpr(new FieldAccessExpr(new ThisExpr(), "code"), new NameExpr("code"), Operator.ASSIGN));
            constructorBody.addStatement(new AssignExpr(new FieldAccessExpr(new ThisExpr(), "message"), new NameExpr("message"), Operator.ASSIGN));
            errorCodeEnumDeclaration.addConstructor(Modifier.PRIVATE).addParameter(PrimitiveType.intType(), "code").addParameter(String.class, "message").setBody(constructorBody);
            errorCodeEnumDeclaration.addField(PrimitiveType.intType(), "code", Modifier.PRIVATE, Modifier.FINAL).createGetter();
            errorCodeEnumDeclaration.addField(String.class, "message", Modifier.PRIVATE, Modifier.FINAL).createGetter();
            _fillErrorCodeEnumDeclaration(errorCodeEnumDeclaration, engineDefinition);
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

    private static void _fillErrorCodeEnumDeclaration(EnumDeclaration errorCodeEnumDeclaration, EngineDefinition engineDefinition) {
        for (ErrorCodeDefinition errorCodeDefinition : engineDefinition.getErrorCodeDefinitions()) {
            errorCodeEnumDeclaration.addEnumConstant(errorCodeDefinition.getName()).setJavadocComment(errorCodeDefinition.getMessage()).addArgument(new IntegerLiteralExpr(errorCodeDefinition.getCode())).addArgument(new StringLiteralExpr(errorCodeDefinition.getMessage()));
        }
    }

    private static void _updateApiJavaFile(String srcFolder, ApiDefinition apiDefinition, EngineDefinition engineDefinition) throws IOException {
        String javaPackageName = engineDefinition.getJavaPackageName();
        File javaFile = new File(_buildJavaFilePath(srcFolder, javaPackageName, apiDefinition.getJavaClassName()));
        CompilationUnit cu = null;
        if (javaFile.exists()) {
            cu = JavaParser.parse(javaFile, encoding);
            ClassOrInterfaceDeclaration apiClassDeclaration = _getApiClassDeclaration(cu);
            Optional<AnnotationExpr> commentAnnotation = apiClassDeclaration.getAnnotationByClass(Comment.class);
            if (commentAnnotation.isPresent()) {
                apiClassDeclaration.remove(commentAnnotation.get());
            }
            apiClassDeclaration.addAndGetAnnotation(Comment.class).addPair("value", "\"" + apiDefinition.getComment() + "\"");
            _deleteMicroApiAnnotation(apiClassDeclaration);
            _fillMicroApiAnnotation(apiClassDeclaration, apiDefinition, engineDefinition);
            _deleteRequestAndResponseClassDeclaration(apiClassDeclaration);
            _fillReqAndRespInnerClassDeclaration(apiClassDeclaration, apiDefinition);
        } else {
            javaFile.getParentFile().mkdirs();
            javaFile.createNewFile();
            cu = new CompilationUnit(javaPackageName);
            if (apiDefinition.getImports() != null) {
                for (String importJavaClass : apiDefinition.getImports()) {
                    cu.addImport(importJavaClass);
                }
            }
            ClassOrInterfaceDeclaration apiClassDeclaration = cu.addClass(apiDefinition.getJavaClassName(), Modifier.PUBLIC);
            apiClassDeclaration.addAndGetAnnotation(Comment.class).addPair("value", "\"" + apiDefinition.getComment() + "\"");
            _fillMicroApiAnnotation(apiClassDeclaration, apiDefinition, engineDefinition);
            _fillExecuteMethodDeclaration(apiClassDeclaration, apiDefinition);
            _fillReqAndRespInnerClassDeclaration(apiClassDeclaration, apiDefinition);
        }
        OutputStream output = new FileOutputStream(javaFile);
        IOUtils.write(cu.toString(), output, encoding);
        IOUtils.closeQuietly(output);
    }

    private static ClassOrInterfaceDeclaration _getApiClassDeclaration(CompilationUnit cu) {
        for (ClassOrInterfaceDeclaration x : cu.getChildNodesByType(ClassOrInterfaceDeclaration.class)) {
            if (x.isAnnotationPresent(MicroApiAnnotation.class)) {
                return x;
            }
        }
        return null;
    }

    private static void _fillReqAndRespInnerClassDeclaration(ClassOrInterfaceDeclaration apiClassDeclaration, ApiDefinition apiDefinition) {
        if (apiDefinition.getRequestDefinition() != null) {
            ClassOrInterfaceDeclaration reqInnerClassDeclaration = new ClassOrInterfaceDeclaration();
            reqInnerClassDeclaration.addModifier(Modifier.PUBLIC, Modifier.STATIC).setName("Req").addExtendedType(Request.class);
            apiClassDeclaration.addMember(reqInnerClassDeclaration);
            _fillFields(reqInnerClassDeclaration, apiDefinition.getRequestDefinition());
        }
        if (apiDefinition.getResponseDefinition() != null) {
            ClassOrInterfaceDeclaration respInnerClassDeclaration = new ClassOrInterfaceDeclaration();
            respInnerClassDeclaration.addModifier(Modifier.PUBLIC, Modifier.STATIC).setName("Resp").addExtendedType(Response.class);
            apiClassDeclaration.addMember(respInnerClassDeclaration);
            _fillFields(respInnerClassDeclaration, apiDefinition.getResponseDefinition());
        }
    }

    private static void _fillExecuteMethodDeclaration(ClassOrInterfaceDeclaration apiClassDeclaration, ApiDefinition apiDefinition) {
        MethodDeclaration executeMethodDeclaration = apiClassDeclaration.addMethod("execute", Modifier.PUBLIC, Modifier.STATIC);
        executeMethodDeclaration.setType(Response.class);
        executeMethodDeclaration.addParameter(Request.class, "request");
        executeMethodDeclaration.addThrownException(Exception.class);
        BlockStmt blockStmt = new BlockStmt();
        if (apiDefinition.getRequestDefinition() != null) {
            blockStmt.addStatement(new AssignExpr(new VariableDeclarationExpr(new ClassOrInterfaceType("Req"), "req"), new CastExpr(new ClassOrInterfaceType("Req"), new NameExpr("request")), Operator.ASSIGN));
        }
        if (apiDefinition.getResponseDefinition() != null) {
            blockStmt.addStatement(new AssignExpr(new VariableDeclarationExpr(new ClassOrInterfaceType("Resp"), "resp"), new ObjectCreationExpr().setType(new ClassOrInterfaceType("Resp")), Operator.ASSIGN));
        } else {
            blockStmt.addStatement(new AssignExpr(new VariableDeclarationExpr(new ClassOrInterfaceType("Response"), "resp"), new ObjectCreationExpr().setType(new ClassOrInterfaceType("Response")), Operator.ASSIGN));
        }
        blockStmt.addStatement(new ReturnStmt(new NameExpr("resp")));
        executeMethodDeclaration.setBody(blockStmt);
    }

    private static void _deleteMicroApiAnnotation(TypeDeclaration<?> typeDeclaration) {
        Optional<AnnotationExpr> optional = typeDeclaration.getAnnotationByClass(MicroApiAnnotation.class);
        if (optional.isPresent()) {
            typeDeclaration.remove(optional.get());
        }
    }

    private static void _fillMicroApiAnnotation(TypeDeclaration<?> typeDeclaration, ApiDefinition apiDefinition, EngineDefinition engineDefinition) {
        typeDeclaration.addAndGetAnnotation(MicroApiAnnotation.class).addPair("type", "\"" + apiDefinition.getType() + "\"").addPair("version", "\"" + engineDefinition.getVersion() + "\"");
    }

    private static void _deleteRequestAndResponseClassDeclaration(ClassOrInterfaceDeclaration apiClassDeclaration) {
        for (ClassOrInterfaceDeclaration x : apiClassDeclaration.getChildNodesByType(ClassOrInterfaceDeclaration.class)) {
            if (x.getExtendedTypes().contains(new ClassOrInterfaceType("Request")) || x.getExtendedTypes().contains(new ClassOrInterfaceType("Response"))) {
                apiClassDeclaration.remove(x);
            }
        }
    }

    private static void _fillFields(ClassOrInterfaceDeclaration classDeclaration, EntityDefinition entityDefinition) {
        for (FieldDefinition fieldDefinition : entityDefinition.getFieldDefinitions()) {
            FieldDeclaration fieldDeclaration = classDeclaration.addPrivateField(fieldDefinition.getJavaType(), fieldDefinition.getName());
            fieldDeclaration.createGetter();
            fieldDeclaration.createSetter();
            if (StringUtils.isNotBlank(fieldDefinition.getComment())) {
                fieldDeclaration.addAndGetAnnotation(Comment.class).addPair("value", "\"" + fieldDefinition.getComment() + "\"");
            }
            if (StringUtils.isNotBlank(fieldDefinition.getDescription())) {
                fieldDeclaration.addAndGetAnnotation(Description.class).addPair("value", "\"" + fieldDefinition.getDescription() + "\"");
            }
            fieldDeclaration.addAndGetAnnotation(Required.class).addPair("value", String.valueOf(fieldDefinition.getRequired()));
        }
    }

    public static EngineDefinition buildEngineDefinition(String engineConfigFilePath) throws IOException {
        FileInputStream input = new FileInputStream(engineConfigFilePath);
        List<String> lines = IOUtils.readLines(input, encoding);
        IOUtils.closeQuietly(input);
        String json = StringUtils.join(lines, "");
        return JSON.parseObject(json, EngineDefinition.class);
    }
}
