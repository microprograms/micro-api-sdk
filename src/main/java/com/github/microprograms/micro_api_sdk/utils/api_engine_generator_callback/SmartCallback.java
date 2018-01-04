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
import com.github.javaparser.ast.expr.BinaryExpr;
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
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.microprograms.micro_api_runtime.enums.MicroApiReserveResponseCodeEnum;
import com.github.microprograms.micro_api_runtime.exception.MicroApiExecuteException;
import com.github.microprograms.micro_api_sdk.model.ApiDefinition;
import com.github.microprograms.micro_entity_definition_runtime.model.EntityDefinition;
import com.github.microprograms.micro_entity_definition_runtime.model.FieldDefinition;
import com.github.microprograms.micro_oss_core.MicroOss;
import com.github.microprograms.micro_oss_core.model.Field;
import com.github.microprograms.micro_oss_core.model.dml.Condition;
import com.github.microprograms.micro_oss_core.model.dml.PagerRequest;
import com.github.microprograms.micro_oss_core.model.dml.PagerResponse;
import com.github.microprograms.micro_oss_core.model.dml.Sort;
import com.github.microprograms.micro_oss_core.model.dml.Where;

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

    private static FieldDefinition getIdField(EntityDefinition entityDefinition) {
        for (FieldDefinition x : entityDefinition.getFieldDefinitions()) {
            if (x.getName().endsWith("Id")) {
                return x;
            }
        }
        return null;
    }

    protected void queryList(String entityName, ClassOrInterfaceDeclaration apiClassDeclaration, ApiDefinition apiDefinition, CompilationUnit cu) {
        // buildFinalCondition
        if (!existMethod(apiClassDeclaration, "buildFinalCondition", getRequestType(apiDefinition))) {
            cu.addImport(Where.class);
            cu.addImport(Condition.class);
            MethodDeclaration buildFinalConditionMethodDeclaration = apiClassDeclaration.addMethod("buildFinalCondition", Modifier.PRIVATE, Modifier.STATIC);
            buildFinalConditionMethodDeclaration.addParameter(new ClassOrInterfaceType(getRequestType(apiDefinition)), "req");
            buildFinalConditionMethodDeclaration.setType(Condition.class);
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
        cu.addImport(MicroOss.class);
        MethodDeclaration coreMethodDeclaration = apiClassDeclaration.addMethod("core", Modifier.PRIVATE, Modifier.STATIC);
        coreMethodDeclaration.addParameter(new ClassOrInterfaceType(getRequestType(apiDefinition)), "req");
        coreMethodDeclaration.addParameter(new ClassOrInterfaceType(getResponseType(apiDefinition)), "resp");
        coreMethodDeclaration.addThrownException(Exception.class);
        BlockStmt coreMethodBody = new BlockStmt();
        coreMethodBody.addStatement(new AssignExpr(new VariableDeclarationExpr(new ClassOrInterfaceType(PagerRequest.class.getSimpleName()), "pager"), new ObjectCreationExpr(null, new ClassOrInterfaceType(PagerRequest.class.getSimpleName()), NodeList.nodeList(new MethodCallExpr(new NameExpr("req"), "getPageIndex"), new MethodCallExpr(new NameExpr("req"), "getPageSize"))), Operator.ASSIGN));
        coreMethodBody.addStatement(new AssignExpr(new VariableDeclarationExpr(new ClassOrInterfaceType(Condition.class.getSimpleName()), "finalCondition"), new MethodCallExpr(null, new SimpleName("buildFinalCondition"), NodeList.nodeList(new NameExpr("req"))), Operator.ASSIGN));
        coreMethodBody.addStatement(new AssignExpr(new VariableDeclarationExpr(new ClassOrInterfaceType(String.format("List<%s>", Sort.class.getSimpleName())), "sorts"), new MethodCallExpr(null, new SimpleName("buildSort"), NodeList.nodeList(new NameExpr("req"))), Operator.ASSIGN));
        coreMethodBody.addStatement(new MethodCallExpr(new NameExpr("resp"), new SimpleName("setData"), NodeList.nodeList(new MethodCallExpr(new NameExpr(MicroOss.class.getSimpleName()), new SimpleName("queryAll"), NodeList.nodeList(new ClassExpr(new ClassOrInterfaceType(entityName)), new NameExpr("finalCondition"), new NameExpr("sorts"), new NameExpr("pager"))))));
        coreMethodBody.addStatement(new MethodCallExpr(new NameExpr("resp"), new SimpleName("setPager"), NodeList.nodeList(new ObjectCreationExpr(null, new ClassOrInterfaceType(PagerResponse.class.getSimpleName()), NodeList.nodeList(new NameExpr("pager"), new MethodCallExpr(new NameExpr(MicroOss.class.getSimpleName()), new SimpleName("queryCount"), NodeList.nodeList(new ClassExpr(new ClassOrInterfaceType(entityName)), new NameExpr("finalCondition"))))))));
        coreMethodDeclaration.setBody(coreMethodBody);
    }

    protected void queryAll(String entityName, ClassOrInterfaceDeclaration apiClassDeclaration, ApiDefinition apiDefinition, CompilationUnit cu) {
        // buildFinalCondition
        if (!existMethod(apiClassDeclaration, "buildFinalCondition", getRequestType(apiDefinition))) {
            MethodDeclaration buildFinalConditionMethodDeclaration = apiClassDeclaration.addMethod("buildFinalCondition", Modifier.PRIVATE, Modifier.STATIC);
            buildFinalConditionMethodDeclaration.addParameter(new ClassOrInterfaceType(getRequestType(apiDefinition)), "req");
            buildFinalConditionMethodDeclaration.setType(Condition.class);
            BlockStmt buildFinalConditionMethodBody = new BlockStmt();
            buildFinalConditionMethodBody.addStatement(new ReturnStmt(new NullLiteralExpr()));
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
        cu.addImport(MicroOss.class);
        MethodDeclaration coreMethodDeclaration = apiClassDeclaration.addMethod("core", Modifier.PRIVATE, Modifier.STATIC);
        coreMethodDeclaration.addParameter(new ClassOrInterfaceType(getRequestType(apiDefinition)), "req");
        coreMethodDeclaration.addParameter(new ClassOrInterfaceType(getResponseType(apiDefinition)), "resp");
        coreMethodDeclaration.addThrownException(Exception.class);
        BlockStmt coreMethodBody = new BlockStmt();
        coreMethodBody.addStatement(new AssignExpr(new VariableDeclarationExpr(new ClassOrInterfaceType(Condition.class.getSimpleName()), "finalCondition"), new MethodCallExpr(null, new SimpleName("buildFinalCondition"), NodeList.nodeList(new NameExpr("req"))), Operator.ASSIGN));
        coreMethodBody.addStatement(new AssignExpr(new VariableDeclarationExpr(new ClassOrInterfaceType(String.format("List<%s>", Sort.class.getSimpleName())), "sorts"), new MethodCallExpr(null, new SimpleName("buildSort"), NodeList.nodeList(new NameExpr("req"))), Operator.ASSIGN));
        coreMethodBody.addStatement(new MethodCallExpr(new NameExpr("resp"), new SimpleName("setData"), NodeList.nodeList(new MethodCallExpr(new NameExpr(MicroOss.class.getSimpleName()), new SimpleName("queryAll"), NodeList.nodeList(new ClassExpr(new ClassOrInterfaceType(entityName)), new NameExpr("finalCondition"), new NameExpr("sorts"))))));
        coreMethodDeclaration.setBody(coreMethodBody);
    }

    protected void queryDetail(String entityName, ClassOrInterfaceDeclaration apiClassDeclaration, ApiDefinition apiDefinition, CompilationUnit cu) {
        // buildFinalCondition
        if (!existMethod(apiClassDeclaration, "buildFinalCondition", getRequestType(apiDefinition))) {
            cu.addImport(Condition.class);
            MethodDeclaration buildFinalConditionMethodDeclaration = apiClassDeclaration.addMethod("buildFinalCondition", Modifier.PRIVATE, Modifier.STATIC);
            buildFinalConditionMethodDeclaration.addParameter(new ClassOrInterfaceType(getRequestType(apiDefinition)), "req");
            buildFinalConditionMethodDeclaration.setType(Condition.class);
            BlockStmt buildFinalConditionMethodBody = new BlockStmt();
            String idFieldName = getIdField(apiDefinition.getRequestDefinition()).getName();
            buildFinalConditionMethodBody.addStatement(new ReturnStmt(new MethodCallExpr(new NameExpr(Condition.class.getSimpleName()), new SimpleName("build"), NodeList.nodeList(new StringLiteralExpr("id="), new MethodCallExpr(new NameExpr("req"), "get" + StringUtils.capitalize(idFieldName))))));
            buildFinalConditionMethodDeclaration.setBody(buildFinalConditionMethodBody);
        }
        // core
        removeMethod(apiClassDeclaration, "core", getRequestType(apiDefinition), getResponseType(apiDefinition));
        cu.addImport(MicroOss.class);
        MethodDeclaration coreMethodDeclaration = apiClassDeclaration.addMethod("core", Modifier.PRIVATE, Modifier.STATIC);
        coreMethodDeclaration.addParameter(new ClassOrInterfaceType(getRequestType(apiDefinition)), "req");
        coreMethodDeclaration.addParameter(new ClassOrInterfaceType(getResponseType(apiDefinition)), "resp");
        coreMethodDeclaration.addThrownException(Exception.class);
        BlockStmt coreMethodBody = new BlockStmt();
        coreMethodBody.addStatement(new AssignExpr(new VariableDeclarationExpr(new ClassOrInterfaceType(Condition.class.getSimpleName()), "finalCondition"), new MethodCallExpr(null, new SimpleName("buildFinalCondition"), NodeList.nodeList(new NameExpr("req"))), Operator.ASSIGN));
        coreMethodBody.addStatement(new MethodCallExpr(new NameExpr("resp"), new SimpleName("setData"), NodeList.nodeList(new MethodCallExpr(new NameExpr(MicroOss.class.getSimpleName()), new SimpleName("queryObject"), NodeList.nodeList(new ClassExpr(new ClassOrInterfaceType(entityName)), new NameExpr("finalCondition"))))));
        coreMethodDeclaration.setBody(coreMethodBody);
    }

    protected void delete(String entityName, ClassOrInterfaceDeclaration apiClassDeclaration, ApiDefinition apiDefinition, CompilationUnit cu) {
        // getOperator
        if (!existMethod(apiClassDeclaration, "getOperator", getRequestType(apiDefinition))) {
            cu.addImport(com.github.microprograms.micro_api_runtime.model.Operator.class);
            MethodDeclaration getOperatorMethodDeclaration = apiClassDeclaration.addMethod("getOperator", Modifier.PRIVATE, Modifier.STATIC);
            getOperatorMethodDeclaration.addParameter(new ClassOrInterfaceType(getRequestType(apiDefinition)), "req");
            getOperatorMethodDeclaration.setType(String.format("%s<?>", com.github.microprograms.micro_api_runtime.model.Operator.class.getSimpleName()));
            BlockStmt getOperatorMethodBody = new BlockStmt();
            getOperatorMethodBody.addStatement(new ReturnStmt(new NullLiteralExpr()));
            getOperatorMethodDeclaration.setBody(getOperatorMethodBody);
        }
        // buildFinalCondition
        if (!existMethod(apiClassDeclaration, "buildFinalCondition", getRequestType(apiDefinition))) {
            cu.addImport(Condition.class);
            MethodDeclaration buildFinalConditionMethodDeclaration = apiClassDeclaration.addMethod("buildFinalCondition", Modifier.PRIVATE, Modifier.STATIC);
            buildFinalConditionMethodDeclaration.addParameter(new ClassOrInterfaceType(getRequestType(apiDefinition)), "req");
            buildFinalConditionMethodDeclaration.setType(Condition.class);
            BlockStmt buildFinalConditionMethodBody = new BlockStmt();
            String idFieldName = getIdField(apiDefinition.getRequestDefinition()).getName();
            buildFinalConditionMethodBody.addStatement(new ReturnStmt(new MethodCallExpr(new NameExpr(Condition.class.getSimpleName()), new SimpleName("build"), NodeList.nodeList(new StringLiteralExpr("id="), new MethodCallExpr(new NameExpr("req"), "get" + StringUtils.capitalize(idFieldName))))));
            buildFinalConditionMethodDeclaration.setBody(buildFinalConditionMethodBody);
        }
        // core
        removeMethod(apiClassDeclaration, "core", getRequestType(apiDefinition), getResponseType(apiDefinition));
        cu.addImport(MicroOss.class);
        cu.addImport(MicroApiExecuteException.class);
        cu.addImport(MicroApiReserveResponseCodeEnum.class);
        MethodDeclaration coreMethodDeclaration = apiClassDeclaration.addMethod("core", Modifier.PRIVATE, Modifier.STATIC);
        coreMethodDeclaration.addParameter(new ClassOrInterfaceType(getRequestType(apiDefinition)), "req");
        coreMethodDeclaration.addParameter(new ClassOrInterfaceType(getResponseType(apiDefinition)), "resp");
        coreMethodDeclaration.addThrownException(Exception.class);
        BlockStmt coreMethodBody = new BlockStmt();
        coreMethodBody.addStatement(new AssignExpr(new VariableDeclarationExpr(new ClassOrInterfaceType(String.format("%s<?>", com.github.microprograms.micro_api_runtime.model.Operator.class.getSimpleName())), "operator"), new MethodCallExpr(null, new SimpleName("getOperator"), NodeList.nodeList(new NameExpr("req"))), Operator.ASSIGN));
        coreMethodBody.addStatement(new IfStmt(new BinaryExpr(new NameExpr("operator"), new NullLiteralExpr(), BinaryExpr.Operator.EQUALS), new ThrowStmt(new ObjectCreationExpr(null, new ClassOrInterfaceType(MicroApiExecuteException.class.getSimpleName()), NodeList.nodeList(new FieldAccessExpr(new NameExpr(MicroApiReserveResponseCodeEnum.class.getSimpleName()), MicroApiReserveResponseCodeEnum.unknown_operator_exception.name())))), null));
        coreMethodBody.addStatement(new IfStmt(new MethodCallExpr(new NameExpr("operator"), "isPermissionDenied"), new ThrowStmt(new ObjectCreationExpr(null, new ClassOrInterfaceType(MicroApiExecuteException.class.getSimpleName()), NodeList.nodeList(new FieldAccessExpr(new NameExpr(MicroApiReserveResponseCodeEnum.class.getSimpleName()), MicroApiReserveResponseCodeEnum.permission_denied_exception.name())))), null));
        coreMethodBody.addStatement(new AssignExpr(new VariableDeclarationExpr(new ClassOrInterfaceType(Condition.class.getSimpleName()), "finalCondition"), new MethodCallExpr(null, new SimpleName("buildFinalCondition"), NodeList.nodeList(new NameExpr("req"))), Operator.ASSIGN));
        coreMethodBody.addStatement(new MethodCallExpr(new NameExpr(MicroOss.class.getSimpleName()), new SimpleName("deleteObject"), NodeList.nodeList(new ClassExpr(new ClassOrInterfaceType(entityName)), new NameExpr("finalCondition"))));
        coreMethodDeclaration.setBody(coreMethodBody);
    }

    protected void add(String entityName, ClassOrInterfaceDeclaration apiClassDeclaration, ApiDefinition apiDefinition, CompilationUnit cu) {
        // getOperator
        if (!existMethod(apiClassDeclaration, "getOperator", getRequestType(apiDefinition))) {
            cu.addImport(com.github.microprograms.micro_api_runtime.model.Operator.class);
            MethodDeclaration getOperatorMethodDeclaration = apiClassDeclaration.addMethod("getOperator", Modifier.PRIVATE, Modifier.STATIC);
            getOperatorMethodDeclaration.addParameter(new ClassOrInterfaceType(getRequestType(apiDefinition)), "req");
            getOperatorMethodDeclaration.setType(String.format("%s<?>", com.github.microprograms.micro_api_runtime.model.Operator.class.getSimpleName()));
            BlockStmt getOperatorMethodBody = new BlockStmt();
            getOperatorMethodBody.addStatement(new ReturnStmt(new NullLiteralExpr()));
            getOperatorMethodDeclaration.setBody(getOperatorMethodBody);
        }
        // buildEntity
        String buildEntityMethodName = String.format("build%s", entityName);
        if (!existMethod(apiClassDeclaration, buildEntityMethodName, getRequestType(apiDefinition))) {
            MethodDeclaration buildEntityMethodDeclaration = apiClassDeclaration.addMethod(buildEntityMethodName, Modifier.PRIVATE, Modifier.STATIC);
            buildEntityMethodDeclaration.addParameter(new ClassOrInterfaceType(getRequestType(apiDefinition)), "req");
            buildEntityMethodDeclaration.setType(entityName);
            BlockStmt buildEntityMethodBody = new BlockStmt();
            buildEntityMethodBody.addStatement(new ReturnStmt(new NullLiteralExpr()));
            buildEntityMethodDeclaration.setBody(buildEntityMethodBody);
        }
        // core
        removeMethod(apiClassDeclaration, "core", getRequestType(apiDefinition), getResponseType(apiDefinition));
        cu.addImport(MicroOss.class);
        cu.addImport(MicroApiExecuteException.class);
        cu.addImport(MicroApiReserveResponseCodeEnum.class);
        MethodDeclaration coreMethodDeclaration = apiClassDeclaration.addMethod("core", Modifier.PRIVATE, Modifier.STATIC);
        coreMethodDeclaration.addParameter(new ClassOrInterfaceType(getRequestType(apiDefinition)), "req");
        coreMethodDeclaration.addParameter(new ClassOrInterfaceType(getResponseType(apiDefinition)), "resp");
        coreMethodDeclaration.addThrownException(Exception.class);
        BlockStmt coreMethodBody = new BlockStmt();
        coreMethodBody.addStatement(new AssignExpr(new VariableDeclarationExpr(new ClassOrInterfaceType(String.format("%s<?>", com.github.microprograms.micro_api_runtime.model.Operator.class.getSimpleName())), "operator"), new MethodCallExpr(null, new SimpleName("getOperator"), NodeList.nodeList(new NameExpr("req"))), Operator.ASSIGN));
        coreMethodBody.addStatement(new IfStmt(new BinaryExpr(new NameExpr("operator"), new NullLiteralExpr(), BinaryExpr.Operator.EQUALS), new ThrowStmt(new ObjectCreationExpr(null, new ClassOrInterfaceType(MicroApiExecuteException.class.getSimpleName()), NodeList.nodeList(new FieldAccessExpr(new NameExpr(MicroApiReserveResponseCodeEnum.class.getSimpleName()), MicroApiReserveResponseCodeEnum.unknown_operator_exception.name())))), null));
        coreMethodBody.addStatement(new IfStmt(new MethodCallExpr(new NameExpr("operator"), "isPermissionDenied"), new ThrowStmt(new ObjectCreationExpr(null, new ClassOrInterfaceType(MicroApiExecuteException.class.getSimpleName()), NodeList.nodeList(new FieldAccessExpr(new NameExpr(MicroApiReserveResponseCodeEnum.class.getSimpleName()), MicroApiReserveResponseCodeEnum.permission_denied_exception.name())))), null));
        coreMethodBody.addStatement(new MethodCallExpr(new NameExpr(MicroOss.class.getSimpleName()), new SimpleName("insertObject"), NodeList.nodeList(new MethodCallExpr(null, new SimpleName(buildEntityMethodName), NodeList.nodeList(new NameExpr("req"))))));
        coreMethodDeclaration.setBody(coreMethodBody);
    }

    protected void update(String entityName, ClassOrInterfaceDeclaration apiClassDeclaration, ApiDefinition apiDefinition, CompilationUnit cu) {
        // getOperator
        if (!existMethod(apiClassDeclaration, "getOperator", getRequestType(apiDefinition))) {
            cu.addImport(com.github.microprograms.micro_api_runtime.model.Operator.class);
            MethodDeclaration getOperatorMethodDeclaration = apiClassDeclaration.addMethod("getOperator", Modifier.PRIVATE, Modifier.STATIC);
            getOperatorMethodDeclaration.addParameter(new ClassOrInterfaceType(getRequestType(apiDefinition)), "req");
            getOperatorMethodDeclaration.setType(String.format("%s<?>", com.github.microprograms.micro_api_runtime.model.Operator.class.getSimpleName()));
            BlockStmt getOperatorMethodBody = new BlockStmt();
            getOperatorMethodBody.addStatement(new ReturnStmt(new NullLiteralExpr()));
            getOperatorMethodDeclaration.setBody(getOperatorMethodBody);
        }
        // buildFinalCondition
        if (!existMethod(apiClassDeclaration, "buildFinalCondition", getRequestType(apiDefinition))) {
            cu.addImport(Condition.class);
            MethodDeclaration buildFinalConditionMethodDeclaration = apiClassDeclaration.addMethod("buildFinalCondition", Modifier.PRIVATE, Modifier.STATIC);
            buildFinalConditionMethodDeclaration.addParameter(new ClassOrInterfaceType(getRequestType(apiDefinition)), "req");
            buildFinalConditionMethodDeclaration.setType(Condition.class);
            BlockStmt buildFinalConditionMethodBody = new BlockStmt();
            String idFieldName = getIdField(apiDefinition.getRequestDefinition()).getName();
            buildFinalConditionMethodBody.addStatement(new ReturnStmt(new MethodCallExpr(new NameExpr(Condition.class.getSimpleName()), new SimpleName("build"), NodeList.nodeList(new StringLiteralExpr("id="), new MethodCallExpr(new NameExpr("req"), "get" + StringUtils.capitalize(idFieldName))))));
            buildFinalConditionMethodDeclaration.setBody(buildFinalConditionMethodBody);
        }
        // buildFieldsToUpdate
        if (!existMethod(apiClassDeclaration, "buildFieldsToUpdate", getRequestType(apiDefinition))) {
            cu.addImport(Field.class);
            cu.addImport(List.class);
            MethodDeclaration buildFieldsToUpdateMethodDeclaration = apiClassDeclaration.addMethod("buildFieldsToUpdate", Modifier.PRIVATE, Modifier.STATIC);
            buildFieldsToUpdateMethodDeclaration.addParameter(new ClassOrInterfaceType(getRequestType(apiDefinition)), "req");
            buildFieldsToUpdateMethodDeclaration.setType(String.format("List<%s>", Field.class.getSimpleName()));
            BlockStmt buildFieldsToUpdateMethodBody = new BlockStmt();
            buildFieldsToUpdateMethodBody.addStatement(new ReturnStmt(new NullLiteralExpr()));
            buildFieldsToUpdateMethodDeclaration.setBody(buildFieldsToUpdateMethodBody);
        }
        // core
        removeMethod(apiClassDeclaration, "core", getRequestType(apiDefinition), getResponseType(apiDefinition));
        cu.addImport(MicroOss.class);
        cu.addImport(MicroApiExecuteException.class);
        cu.addImport(MicroApiReserveResponseCodeEnum.class);
        MethodDeclaration coreMethodDeclaration = apiClassDeclaration.addMethod("core", Modifier.PRIVATE, Modifier.STATIC);
        coreMethodDeclaration.addParameter(new ClassOrInterfaceType(getRequestType(apiDefinition)), "req");
        coreMethodDeclaration.addParameter(new ClassOrInterfaceType(getResponseType(apiDefinition)), "resp");
        coreMethodDeclaration.addThrownException(Exception.class);
        BlockStmt coreMethodBody = new BlockStmt();
        coreMethodBody.addStatement(new AssignExpr(new VariableDeclarationExpr(new ClassOrInterfaceType(String.format("%s<?>", com.github.microprograms.micro_api_runtime.model.Operator.class.getSimpleName())), "operator"), new MethodCallExpr(null, new SimpleName("getOperator"), NodeList.nodeList(new NameExpr("req"))), Operator.ASSIGN));
        coreMethodBody.addStatement(new IfStmt(new BinaryExpr(new NameExpr("operator"), new NullLiteralExpr(), BinaryExpr.Operator.EQUALS), new ThrowStmt(new ObjectCreationExpr(null, new ClassOrInterfaceType(MicroApiExecuteException.class.getSimpleName()), NodeList.nodeList(new FieldAccessExpr(new NameExpr(MicroApiReserveResponseCodeEnum.class.getSimpleName()), MicroApiReserveResponseCodeEnum.unknown_operator_exception.name())))), null));
        coreMethodBody.addStatement(new IfStmt(new MethodCallExpr(new NameExpr("operator"), "isPermissionDenied"), new ThrowStmt(new ObjectCreationExpr(null, new ClassOrInterfaceType(MicroApiExecuteException.class.getSimpleName()), NodeList.nodeList(new FieldAccessExpr(new NameExpr(MicroApiReserveResponseCodeEnum.class.getSimpleName()), MicroApiReserveResponseCodeEnum.permission_denied_exception.name())))), null));
        coreMethodBody.addStatement(new AssignExpr(new VariableDeclarationExpr(new ClassOrInterfaceType(Condition.class.getSimpleName()), "finalCondition"), new MethodCallExpr(null, new SimpleName("buildFinalCondition"), NodeList.nodeList(new NameExpr("req"))), Operator.ASSIGN));
        coreMethodBody.addStatement(new AssignExpr(new VariableDeclarationExpr(new ClassOrInterfaceType(String.format("List<%s>", Field.class.getSimpleName())), "fields"), new MethodCallExpr(null, new SimpleName("buildFieldsToUpdate"), NodeList.nodeList(new NameExpr("req"))), Operator.ASSIGN));
        coreMethodBody.addStatement(new MethodCallExpr(new NameExpr(MicroOss.class.getSimpleName()), new SimpleName("updateObject"), NodeList.nodeList(new ClassExpr(new ClassOrInterfaceType(entityName)), new NameExpr("fields"), new NameExpr("finalCondition"))));
        coreMethodDeclaration.setBody(coreMethodBody);
    }
}
