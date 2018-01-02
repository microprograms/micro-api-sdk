package com.github.microprograms.micro_api_sdk.utils.api_engine_generator_callback;

import org.apache.commons.lang3.StringUtils;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.microprograms.micro_api_runtime.enums.MicroApiReserveResponseCodeEnum;
import com.github.microprograms.micro_api_runtime.exception.MicroApiExecuteException;
import com.github.microprograms.micro_api_runtime.model.Request;
import com.github.microprograms.micro_api_runtime.model.Response;
import com.github.microprograms.micro_api_runtime.utils.MicroApiUtils;
import com.github.microprograms.micro_api_sdk.model.ApiDefinition;
import com.github.microprograms.micro_entity_definition_runtime.model.FieldDefinition;

public class DefaultCallback implements Callback {

    @Override
    public void updateExecuteMethodDeclaration(ClassOrInterfaceDeclaration apiClassDeclaration, ApiDefinition apiDefinition, CompilationUnit cu) {
        removeMethod(apiClassDeclaration, "execute", "Request");
        MethodDeclaration executeMethodDeclaration = apiClassDeclaration.addMethod("execute", Modifier.PUBLIC, Modifier.STATIC);
        executeMethodDeclaration.setType(Response.class);
        executeMethodDeclaration.addParameter(Request.class, "request");
        executeMethodDeclaration.addThrownException(Exception.class);
        BlockStmt blockStmt = new BlockStmt();
        if (apiDefinition.getRequestDefinition() != null) {
            blockStmt.addStatement(new AssignExpr(new VariableDeclarationExpr(new ClassOrInterfaceType("Req"), "req"), new CastExpr(new ClassOrInterfaceType("Req"), new NameExpr("request")), Operator.ASSIGN));
            for (FieldDefinition x : apiDefinition.getRequestDefinition().getFieldDefinitions()) {
                if (x.getRequired()) {
                    cu.addImport(MicroApiUtils.class.getName());
                    NodeList<Expression> arguments = new NodeList<>();
                    arguments.add(new MethodCallExpr(new NameExpr("req"), "get" + StringUtils.capitalize(x.getName())));
                    arguments.add(new StringLiteralExpr(x.getName()));
                    blockStmt.addStatement(new MethodCallExpr(new NameExpr(MicroApiUtils.class.getSimpleName()), new SimpleName("throwExceptionIfBlank"), arguments));
                }
            }
        } else {
            blockStmt.addStatement(new AssignExpr(new VariableDeclarationExpr(new ClassOrInterfaceType("Request"), "req"), new NameExpr("request"), Operator.ASSIGN));
        }
        if (apiDefinition.getResponseDefinition() != null) {
            blockStmt.addStatement(new AssignExpr(new VariableDeclarationExpr(new ClassOrInterfaceType("Resp"), "resp"), new ObjectCreationExpr().setType(new ClassOrInterfaceType("Resp")), Operator.ASSIGN));
        } else {
            blockStmt.addStatement(new AssignExpr(new VariableDeclarationExpr(new ClassOrInterfaceType("Response"), "resp"), new ObjectCreationExpr().setType(new ClassOrInterfaceType("Response")), Operator.ASSIGN));
        }
        blockStmt.addStatement(new MethodCallExpr(null, new SimpleName("core"), NodeList.nodeList(new NameExpr("req"), new NameExpr("resp"))));
        blockStmt.addStatement(new ReturnStmt(new NameExpr("resp")));
        executeMethodDeclaration.setBody(blockStmt);
    }

    @Override
    public void updateCoreMethodDeclaration(ClassOrInterfaceDeclaration apiClassDeclaration, ApiDefinition apiDefinition, CompilationUnit cu) {
        if (existMethod(apiClassDeclaration, "core", getRequestType(apiDefinition), getResponseType(apiDefinition))) {
            return;
        }
        cu.addImport(MicroApiExecuteException.class.getName());
        cu.addImport(MicroApiReserveResponseCodeEnum.class.getName());
        MethodDeclaration methodDeclaration = apiClassDeclaration.addMethod("core", Modifier.PRIVATE, Modifier.STATIC);
        methodDeclaration.addParameter(new ClassOrInterfaceType(getRequestType(apiDefinition)), "req");
        methodDeclaration.addParameter(new ClassOrInterfaceType(getResponseType(apiDefinition)), "resp");
        methodDeclaration.addThrownException(Exception.class);
        BlockStmt blockStmt = new BlockStmt();
        blockStmt.addStatement(new VariableDeclarationExpr(new ClassOrInterfaceType(Object.class.getSimpleName()), "doSomeThingHere"));
        blockStmt.addStatement(new ThrowStmt(new ObjectCreationExpr(null, new ClassOrInterfaceType(MicroApiExecuteException.class.getSimpleName()), NodeList.nodeList(new FieldAccessExpr(new NameExpr(MicroApiReserveResponseCodeEnum.class.getSimpleName()), MicroApiReserveResponseCodeEnum.api_not_implemented_exception.name())))));
        methodDeclaration.setBody(blockStmt);
    }

    protected void removeMethod(ClassOrInterfaceDeclaration apiClassDeclaration, String name, String... paramTypes) {
        for (MethodDeclaration x : apiClassDeclaration.getMethodsBySignature(name, paramTypes)) {
            x.remove();
        }
    }

    protected boolean existMethod(ClassOrInterfaceDeclaration apiClassDeclaration, String name, String... paramTypes) {
        return !apiClassDeclaration.getMethodsBySignature(name, paramTypes).isEmpty();
    }

    protected String getRequestType(ApiDefinition apiDefinition) {
        return apiDefinition.getRequestDefinition() != null ? "Req" : "Request";
    }

    protected String getResponseType(ApiDefinition apiDefinition) {
        return apiDefinition.getResponseDefinition() != null ? "Resp" : "Response";
    }
}
