package com.github.microprograms.micro_api_sdk.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.microprograms.micro_api_sdk.model.ApiDefinition;
import com.github.microprograms.micro_api_sdk.model.ModuleDefinition;
import com.github.microprograms.osgi_module_activator.ModuleActivator;

public class OsgiModule {
    private static final Charset encoding = Charset.forName("utf8");

    /**
     * 更新activator类
     */
    public static class UpdateActivatorJavaSourceFile {
        public static final String activator_class_name = "Activator";
        public static final String onStart_method_name = "onStart";
        public static final String registerApis_method_name = "registerApis";

        public static void update(ModuleDefinition moduleDefinition, String srcFolder, String javaPackageName,
                String apiJavaPackageName) throws IOException {
            File javaFile = JavaParserUtils.buildJavaSourceFile(srcFolder, javaPackageName, activator_class_name);
            CompilationUnit cu = null;
            ClassOrInterfaceDeclaration activatorClass = null;
            if (javaFile.exists()) {
                cu = JavaParser.parse(javaFile, encoding);
                activatorClass = cu.getClassByName(activator_class_name).get();
            } else {
                javaFile.getParentFile().mkdirs();
                javaFile.createNewFile();
                cu = new CompilationUnit(javaPackageName);
                cu.addImport(apiJavaPackageName, false, true);
                activatorClass = cu.addClass(activator_class_name, Modifier.PUBLIC)
                        .addExtendedType(ModuleActivator.class);
            }
            _updateOnStartMethod(activatorClass);
            _deleteRegisterApisMethod(activatorClass);
            _addRegisterApisMethod(activatorClass, moduleDefinition);
            JavaParserUtils.write(cu, javaFile, encoding);
        }

        private static void _updateOnStartMethod(ClassOrInterfaceDeclaration activatorClass) {
            if (!activatorClass.getMethodsBySignature(onStart_method_name).isEmpty()) {
                return;
            }
            MethodDeclaration method = activatorClass.addMethod(onStart_method_name, Modifier.PROTECTED)
                    .addThrownException(Exception.class).addMarkerAnnotation(Override.class);
            BlockStmt block = new BlockStmt();
            block.addStatement(new MethodCallExpr().setName(registerApis_method_name));
            method.setBody(block);
        }

        private static void _deleteRegisterApisMethod(ClassOrInterfaceDeclaration activatorClass) {
            for (MethodDeclaration x : activatorClass.getMethodsBySignature(registerApis_method_name)) {
                activatorClass.remove(x);
            }
        }

        private static void _addRegisterApisMethod(ClassOrInterfaceDeclaration activatorClass,
                ModuleDefinition moduleDefinition) {
            MethodDeclaration method = activatorClass.addMethod(registerApis_method_name, Modifier.PRIVATE);
            BlockStmt block = new BlockStmt();
            for (ApiDefinition x : moduleDefinition.getApiDefinitions()) {
                block.addStatement(new MethodCallExpr().setName("registerApi")
                        .addArgument(new ObjectCreationExpr().setType(new ClassOrInterfaceType(x.getName()))));
            }
            method.setBody(block);
        }
    }
}