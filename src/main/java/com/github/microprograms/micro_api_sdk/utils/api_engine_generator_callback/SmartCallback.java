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
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.microprograms.ignite_utils.IgniteUtils;
import com.github.microprograms.ignite_utils.sql.dml.PagerRequest;
import com.github.microprograms.ignite_utils.sql.dml.PagerResponse;
import com.github.microprograms.ignite_utils.sql.dml.Sort;
import com.github.microprograms.ignite_utils.sql.dml.Where;
import com.github.microprograms.micro_api_runtime.enums.MicroApiReserveResponseCodeEnum;
import com.github.microprograms.micro_api_runtime.exception.MicroApiExecuteException;
import com.github.microprograms.micro_api_sdk.model.ApiDefinition;

public class SmartCallback extends DefaultCallback {

    @Override
    public void fillCoreMethodDeclaration(ClassOrInterfaceDeclaration apiClassDeclaration, ApiDefinition apiDefinition, CompilationUnit cu) {
        String[] javaClassNames = apiDefinition.getJavaClassName().split("_");
        String entityName = javaClassNames[0];
        String keyword = javaClassNames[1];
        Method method = getMethod(keyword);
        if (method == null) {
            super.fillCoreMethodDeclaration(apiClassDeclaration, apiDefinition, cu);
        }
        try {
            method.invoke(null, entityName, apiClassDeclaration, apiDefinition, cu);
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
    private static void queryList(String entityName, ClassOrInterfaceDeclaration apiClassDeclaration, ApiDefinition apiDefinition, CompilationUnit cu) {
        // core
        cu.addImport(PagerRequest.class);
        cu.addImport(PagerResponse.class);
        cu.addImport(IgniteUtils.class);
        MethodDeclaration coreMethodDeclaration = apiClassDeclaration.addMethod("core", Modifier.PRIVATE, Modifier.STATIC);
        coreMethodDeclaration.addParameter(new ClassOrInterfaceType(apiDefinition.getRequestDefinition() != null ? "Req" : "Request"), "req");
        coreMethodDeclaration.addParameter(new ClassOrInterfaceType(apiDefinition.getResponseDefinition() != null ? "Resp" : "Response"), "resp");
        coreMethodDeclaration.addThrownException(Exception.class);
        BlockStmt coreMethodBody = new BlockStmt();
        coreMethodBody.addStatement(new AssignExpr(new VariableDeclarationExpr(new ClassOrInterfaceType(PagerRequest.class.getSimpleName()), "pager"), new ObjectCreationExpr(null, new ClassOrInterfaceType(PagerRequest.class.getSimpleName()), NodeList.nodeList(new MethodCallExpr(new NameExpr("req"), "getPageIndex"), new MethodCallExpr(new NameExpr("req"), "getPageSize"))), Operator.ASSIGN));
        coreMethodBody.addStatement(new AssignExpr(new VariableDeclarationExpr(new ClassOrInterfaceType(Object.class.getSimpleName()), "finalCondition"), new MethodCallExpr(null, new SimpleName("buildFinalCondition"), NodeList.nodeList(new NameExpr("req"))), Operator.ASSIGN));
        coreMethodBody.addStatement(new AssignExpr(new VariableDeclarationExpr(new ClassOrInterfaceType(String.format("List<%s>", Sort.class.getSimpleName())), "sorts"), new MethodCallExpr(null, new SimpleName("buildSort"), NodeList.nodeList(new NameExpr("req"))), Operator.ASSIGN));
        coreMethodBody.addStatement(new MethodCallExpr(new NameExpr("resp"), new SimpleName("setData"), NodeList.nodeList(new MethodCallExpr(new NameExpr(IgniteUtils.class.getSimpleName()), new SimpleName("queryAllObject"), NodeList.nodeList(new ClassExpr(new ClassOrInterfaceType(entityName)), new NameExpr("finalCondition"), new NameExpr("sorts"), new NameExpr("pager"))))));
        coreMethodBody.addStatement(new MethodCallExpr(new NameExpr("resp"), new SimpleName("setPager"), NodeList.nodeList(new ObjectCreationExpr(null, new ClassOrInterfaceType(PagerResponse.class.getSimpleName()), NodeList.nodeList(new NameExpr("pager"), new MethodCallExpr(new NameExpr(IgniteUtils.class.getSimpleName()), new SimpleName("queryCount"), NodeList.nodeList(new ClassExpr(new ClassOrInterfaceType(entityName)), new NameExpr("finalCondition"))))))));
        coreMethodBody.addStatement(new ThrowStmt(new ObjectCreationExpr(null, new ClassOrInterfaceType(MicroApiExecuteException.class.getSimpleName()), NodeList.nodeList(new FieldAccessExpr(new NameExpr(MicroApiReserveResponseCodeEnum.class.getSimpleName()), MicroApiReserveResponseCodeEnum.api_not_implemented_exception.name())))));
        coreMethodDeclaration.setBody(coreMethodBody);
        // buildFinalCondition
        cu.addImport(Where.class);
        MethodDeclaration buildFinalConditionMethodDeclaration = apiClassDeclaration.addMethod("buildFinalCondition", Modifier.PRIVATE, Modifier.STATIC);
        buildFinalConditionMethodDeclaration.addParameter(new ClassOrInterfaceType(apiDefinition.getRequestDefinition() != null ? "Req" : "Request"), "req");
        buildFinalConditionMethodDeclaration.setType(Object.class);
        BlockStmt buildFinalConditionMethodBody = new BlockStmt();
        buildFinalConditionMethodBody.addStatement(new ReturnStmt(new MethodCallExpr(new NameExpr(Where.class.getSimpleName()), new SimpleName("and"), NodeList.nodeList(new NullLiteralExpr()))));
        buildFinalConditionMethodDeclaration.setBody(buildFinalConditionMethodBody);
        // buildSort
        cu.addImport(List.class);
        cu.addImport(Sort.class);
        cu.addImport(Arrays.class);
        MethodDeclaration buildSortMethodDeclaration = apiClassDeclaration.addMethod("buildSort", Modifier.PRIVATE, Modifier.STATIC);
        buildSortMethodDeclaration.addParameter(new ClassOrInterfaceType(apiDefinition.getRequestDefinition() != null ? "Req" : "Request"), "req");
        buildSortMethodDeclaration.setType(String.format("List<%s>", Sort.class.getSimpleName()));
        BlockStmt buildSortMethodBody = new BlockStmt();
        buildSortMethodBody.addStatement(new ReturnStmt(new MethodCallExpr(new NameExpr(Arrays.class.getSimpleName()), new SimpleName("asList"), NodeList.nodeList(new MethodCallExpr(new NameExpr(Sort.class.getSimpleName()), new SimpleName("desc"), NodeList.nodeList(new StringLiteralExpr("dtCreate")))))));
        buildSortMethodDeclaration.setBody(buildSortMethodBody);
    }
}
