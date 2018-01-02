package com.github.microprograms.micro_api_sdk.utils.api_engine_generator_callback;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.microprograms.micro_api_sdk.model.ApiDefinition;

public interface Callback {
    void fillExecuteMethodDeclaration(ClassOrInterfaceDeclaration apiClassDeclaration, ApiDefinition apiDefinition, CompilationUnit cu);

    void fillCoreMethodDeclaration(ClassOrInterfaceDeclaration apiClassDeclaration, ApiDefinition apiDefinition, CompilationUnit cu);
}
