package com.github.microprograms.micro_api_sdk.utils.api_engine_generator_callback;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.microprograms.ignite_utils.IgniteUtils;
import com.github.microprograms.ignite_utils.sql.dml.PagerRequest;
import com.github.microprograms.ignite_utils.sql.dml.PagerResponse;
import com.github.microprograms.ignite_utils.sql.dml.Sort;
import com.github.microprograms.ignite_utils.sql.dml.Where;
import com.github.microprograms.micro_api_sdk.model.ApiDefinition;

public class SmartCallback extends DefaultCallback {

    @Override
    public void updateCoreMethodDeclaration(ClassOrInterfaceDeclaration apiClassDeclaration, ApiDefinition apiDefinition, CompilationUnit cu) {
        String[] javaClassNames = apiDefinition.getJavaClassName().split("_");
        String entityName = javaClassNames[0];
        String keyword = javaClassNames[1];
        Method method = getMethod(keyword);
        if (method == null) {
            super.updateCoreMethodDeclaration(apiClassDeclaration, apiDefinition, cu);
            return;
        }
        try {
            method.invoke(this, entityName, apiClassDeclaration, apiDefinition, cu);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static Method getMethod(String keyword) {
        try {
            return SmartCallback.class.getDeclaredMethod(StringUtils.uncapitalize(keyword), String.class, ClassOrInterfaceDeclaration.class, ApiDefinition.class, CompilationUnit.class);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unused")
    private void queryList(String entityName, ClassOrInterfaceDeclaration apiClassDeclaration, ApiDefinition apiDefinition, CompilationUnit cu) {
        // buildFinalCondition
        if (!existMethod(apiClassDeclaration, "buildFinalCondition", getRequestType(apiDefinition))) {
            cu.addImport(Where.class);
            MethodDeclaration buildFinalConditionMethodDeclaration = apiClassDeclaration.addMethod("buildFinalCondition", Modifier.PRIVATE, Modifier.STATIC);
            buildFinalConditionMethodDeclaration.addParameter(new ClassOrInterfaceType(getRequestType(apiDefinition)), "req");
            buildFinalConditionMethodDeclaration.setType(Object.class);
            BlockStmt buildFinalConditionMethodBody = new BlockStmt();
            buildFinalConditionMethodBody.addStatement(new ReturnStmt(new MethodCallExpr(new NameExpr(Where.class.getSimpleName()), new SimpleName("and"), NodeList.nodeList(new NullLiteralExpr()))));
            buildFinalConditionMethodDeclaration.setBody(buildFinalConditionMethodBody);
        }
        // buildSort
        if (!existMethod(apiClassDeclaration, "buildSort", getRequestType(apiDefinition))) {
            cu.addImport(List.class);
            cu.addImport(Sort.class);
            cu.addImport(Arrays.class);
            MethodDeclaration buildSortMethodDeclaration = apiClassDeclaration.addMethod("buildSort", Modifier.PRIVATE, Modifier.STATIC);
            buildSortMethodDeclaration.addParameter(new ClassOrInterfaceType(getRequestType(apiDefinition)), "req");
            buildSortMethodDeclaration.setType(String.format("List<%s>", Sort.class.getSimpleName()));
            BlockStmt buildSortMethodBody = new BlockStmt();
            buildSortMethodBody.addStatement(new ReturnStmt(new MethodCallExpr(new NameExpr(Arrays.class.getSimpleName()), new SimpleName("asList"), NodeList.nodeList(new MethodCallExpr(new NameExpr(Sort.class.getSimpleName()), new SimpleName("desc"), NodeList.nodeList(new StringLiteralExpr("dtCreate")))))));
            buildSortMethodDeclaration.setBody(buildSortMethodBody);
        }
        // core
        removeMethod(apiClassDeclaration, "core", getRequestType(apiDefinition), getResponseType(apiDefinition));
        cu.addImport(PagerRequest.class);
        cu.addImport(PagerResponse.class);
        cu.addImport(IgniteUtils.class);
        MethodDeclaration coreMethodDeclaration = apiClassDeclaration.addMethod("core", Modifier.PRIVATE, Modifier.STATIC);
        coreMethodDeclaration.addParameter(new ClassOrInterfaceType(getRequestType(apiDefinition)), "req");
        coreMethodDeclaration.addParameter(new ClassOrInterfaceType(getResponseType(apiDefinition)), "resp");
        coreMethodDeclaration.addThrownException(Exception.class);
        BlockStmt coreMethodBody = new BlockStmt();
        coreMethodBody.addStatement(new AssignExpr(new VariableDeclarationExpr(new ClassOrInterfaceType(PagerRequest.class.getSimpleName()), "pager"), new ObjectCreationExpr(null, new ClassOrInterfaceType(PagerRequest.class.getSimpleName()), NodeList.nodeList(new MethodCallExpr(new NameExpr("req"), "getPageIndex"), new MethodCallExpr(new NameExpr("req"), "getPageSize"))), Operator.ASSIGN));
        coreMethodBody.addStatement(new AssignExpr(new VariableDeclarationExpr(new ClassOrInterfaceType(Object.class.getSimpleName()), "finalCondition"), new MethodCallExpr(null, new SimpleName("buildFinalCondition"), NodeList.nodeList(new NameExpr("req"))), Operator.ASSIGN));
        coreMethodBody.addStatement(new AssignExpr(new VariableDeclarationExpr(new ClassOrInterfaceType(String.format("List<%s>", Sort.class.getSimpleName())), "sorts"), new MethodCallExpr(null, new SimpleName("buildSort"), NodeList.nodeList(new NameExpr("req"))), Operator.ASSIGN));
        coreMethodBody.addStatement(new MethodCallExpr(new NameExpr("resp"), new SimpleName("setData"), NodeList.nodeList(new MethodCallExpr(new NameExpr(IgniteUtils.class.getSimpleName()), new SimpleName("queryAllObject"), NodeList.nodeList(new ClassExpr(new ClassOrInterfaceType(entityName)), new NameExpr("finalCondition"), new NameExpr("sorts"), new NameExpr("pager"))))));
        coreMethodBody.addStatement(new MethodCallExpr(new NameExpr("resp"), new SimpleName("setPager"), NodeList.nodeList(new ObjectCreationExpr(null, new ClassOrInterfaceType(PagerResponse.class.getSimpleName()), NodeList.nodeList(new NameExpr("pager"), new MethodCallExpr(new NameExpr(IgniteUtils.class.getSimpleName()), new SimpleName("queryCount"), NodeList.nodeList(new ClassExpr(new ClassOrInterfaceType(entityName)), new NameExpr("finalCondition"))))))));
        coreMethodDeclaration.setBody(coreMethodBody);
    }
}
