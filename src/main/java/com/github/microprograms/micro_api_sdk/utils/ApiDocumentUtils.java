package com.github.microprograms.micro_api_sdk.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import com.github.microprograms.micro_api_runtime.enums.MicroApiReserveResponseCodeEnum;
import com.github.microprograms.micro_api_sdk.model.ApiDefinition;
import com.github.microprograms.micro_api_sdk.model.EngineDefinition;
import com.github.microprograms.micro_api_sdk.model.ErrorCodeDefinition;
import com.github.microprograms.micro_entity_definition_runtime.model.EntityDefinition;
import com.github.microprograms.micro_entity_definition_runtime.model.FieldDefinition;
import com.github.xuzw.html_builder.HtmlBuilder;

public class ApiDocumentUtils {
    private static final Charset encoding = Charset.forName("utf8");
    public static final String path_format = "%s/%s-%s.html";

    public static void writeApiHtmlDocumentFile(String documentFolder, EngineDefinition engineDefinition) throws IOException {
        String path = String.format(path_format, documentFolder, engineDefinition.getComment(), engineDefinition.getVersion());
        File file = new File(path);
        file.getParentFile().mkdirs();
        OutputStream output = new FileOutputStream(file);
        IOUtils.write(_buildHtmlPage(engineDefinition), output, encoding);
        IOUtils.closeQuietly(output);
    }

    private static String _buildHtmlPage(EngineDefinition engineDefinition) {
        HtmlBuilder root = new HtmlBuilder();
        root.child("!DOCTYPE").attr("html");
        root.child("html").attr("lang", "zh-CN");
        HtmlBuilder head = root.child("head");
        head.child("meta").attr("http-equiv", "content-type").attr("content", "text/html; charset=UTF-8");
        head.child("meta").attr("charset", "utf-8");
        head.append("title", engineDefinition.getComment() + " " + engineDefinition.getVersion());
        head.child("style").attr("type", "text/css").text(_buildCss());
        HtmlBuilder body = root.child("body");
        // 标题
        HtmlBuilder title = body.child("div");
        title.append("h4", engineDefinition.getComment() + " " + engineDefinition.getVersion());
        // 提示
        HtmlBuilder tips = body.child("h5");
        tips.text(engineDefinition.getDescription());
        // API接口书签
        HtmlBuilder apiBookmarkBox = body.child("div").cssClass("alert alert-warning");
        apiBookmarkBox.append("p", "共" + engineDefinition.getApiDefinitions().size() + "个接口");
        HtmlBuilder apiBookmarks = apiBookmarkBox.child("ul");
        for (ApiDefinition apiDefinition : engineDefinition.getApiDefinitions()) {
            apiBookmarks.child("li").cssClass("api-name-bookmark").child("a").attr("href", "#" + apiDefinition.getJavaClassName()).text(StringEscapeUtils.escapeHtml4(apiDefinition.getComment()));
        }
        // 公共字段
        HtmlBuilder commonFields = body.child("div").cssClass("alert alert-success");
        commonFields.child("dt").cssClass("request-and-response-title").text("公共请求地址");
        commonFields.child("dd").child("span").cssClass("api-field-name").text(engineDefinition.getServerAddressDefinition().toString());
        commonFields.child("dt").cssClass("request-and-response-title").text("公共错误码");
        _appendErrorCodeDefinition(commonFields, engineDefinition);
        // API接口
        for (int i = 0; i < engineDefinition.getApiDefinitions().size(); i++) {
            ApiDefinition apiDefinition = engineDefinition.getApiDefinitions().get(i);
            HtmlBuilder apiBox = body.child("div").cssClass("alert alert-info");
            apiBox.child("a").attr("name", apiDefinition.getJavaClassName());
            apiBox.child("h4").cssClass("api-name").text(StringEscapeUtils.escapeHtml4("#" + (i + 1) + " " + apiDefinition.getComment()));
            if (StringUtils.isNotBlank(apiDefinition.getDescription())) {
                apiBox.child("h5").cssClass("api-name").text(StringEscapeUtils.escapeHtml4(apiDefinition.getDescription()));
            }
            apiBox.child("dt").cssClass("request-and-response-title").text("请求字段");
            _appendRequestDefinition(apiBox, apiDefinition.getRequestDefinition(), apiDefinition, engineDefinition);
            apiBox.child("dt").cssClass("request-and-response-title").text("响应字段");
            _appendResponseDefinition(apiBox, apiDefinition.getResponseDefinition(), engineDefinition);
        }
        // Model
        if (engineDefinition.getModelDefinitions() != null) {
            for (EntityDefinition modelDefinition : engineDefinition.getModelDefinitions()) {
                HtmlBuilder modelBox = body.child("div").cssClass("alert alert-success");
                modelBox.child("a").attr("name", modelDefinition.getJavaClassName());
                StringBuilder modelTitle = new StringBuilder(modelDefinition.getJavaClassName());
                if (StringUtils.isNoneBlank(modelDefinition.getComment())) {
                    modelTitle.append(" ").append(modelDefinition.getComment());
                }
                modelBox.child("h4").cssClass("api-name").text(StringEscapeUtils.escapeHtml4(modelTitle.toString()));
                if (StringUtils.isNotBlank(modelDefinition.getDescription())) {
                    modelBox.child("h5").cssClass("api-name").text(StringEscapeUtils.escapeHtml4(modelDefinition.getDescription()));
                }
                _appendEntityDefinition(modelBox, modelDefinition, engineDefinition);
            }
        }
        // 尾部
        HtmlBuilder tail = body.child("div").cssClass("tail").child("p");
        tail.append("span", "GenerateBy").child("a").href("https://github.com/microprograms/micro-api-sdk").text("MicroApiSdk");
        tail.append("span", _buildTime());
        return root.build();
    }

    private static void _appendRequestDefinition(HtmlBuilder apiBox, EntityDefinition entityDefinition, ApiDefinition apiDefinition, EngineDefinition engineDefinition) {
        List<FieldDefinition> commonFieldDefinitions = new ArrayList<>();
        commonFieldDefinitions.add(_buildFieldDefinition("接口名（固定为" + apiDefinition.getJavaClassName() + "）", "apiName", "String", true));
        if (entityDefinition == null) {
            entityDefinition = new EntityDefinition();
            entityDefinition.setFieldDefinitions(commonFieldDefinitions);
        } else {
            entityDefinition.getFieldDefinitions().addAll(0, commonFieldDefinitions);
        }
        _appendEntityDefinition(apiBox, entityDefinition, engineDefinition);
    }

    private static void _appendResponseDefinition(HtmlBuilder apiBox, EntityDefinition entityDefinition, EngineDefinition engineDefinition) {
        List<FieldDefinition> commonFieldDefinitions = new ArrayList<>();
        commonFieldDefinitions.add(_buildFieldDefinition("错误码", "code", "int", true));
        commonFieldDefinitions.add(_buildFieldDefinition("错误提示", "message", "String", true));
        if (entityDefinition == null) {
            entityDefinition = new EntityDefinition();
            entityDefinition.setFieldDefinitions(commonFieldDefinitions);
        } else {
            entityDefinition.getFieldDefinitions().addAll(0, commonFieldDefinitions);
        }
        _appendEntityDefinition(apiBox, entityDefinition, engineDefinition);
    }

    private static void _appendEntityDefinition(HtmlBuilder apiBox, EntityDefinition entityDefinition, EngineDefinition engineDefinition) {
        for (FieldDefinition fieldDefinition : entityDefinition.getFieldDefinitions()) {
            HtmlBuilder apiField = apiBox.child("dd");
            apiField.child("span").cssClass("api-field-name").text(fieldDefinition.getName());
            _appendEntityFieldTypeDefinition(apiField.child("span").cssClass("api-field-type"), fieldDefinition, engineDefinition);
            apiField.child("span").cssClass("api-field-required").text(fieldDefinition.getRequired() ? "必填" : "可选");
            apiField.child("span").cssClass("api-field-comment").text(StringEscapeUtils.escapeHtml4(fieldDefinition.getComment()));
        }
    }

    private static void _appendErrorCodeDefinition(HtmlBuilder container, EngineDefinition engineDefinition) {
        List<ErrorCodeDefinition> errorCodeDefinitions = new ArrayList<>();
        errorCodeDefinitions.addAll(engineDefinition.getErrorCodeDefinitions());
        for (MicroApiReserveResponseCodeEnum x : MicroApiReserveResponseCodeEnum.values()) {
            ErrorCodeDefinition errorCodeDefinition = new ErrorCodeDefinition();
            errorCodeDefinition.setCode(x.getCode());
            errorCodeDefinition.setMessage(x.getMessage());
            errorCodeDefinition.setName(x.toString());
            errorCodeDefinitions.add(errorCodeDefinition);
        }
        Collections.sort(errorCodeDefinitions, new Comparator<ErrorCodeDefinition>() {
            @Override
            public int compare(ErrorCodeDefinition o1, ErrorCodeDefinition o2) {
                return o1.getCode() - o2.getCode();
            }
        });
        for (ErrorCodeDefinition errorCodeDefinition : errorCodeDefinitions) {
            HtmlBuilder errorCode = container.child("dd");
            errorCode.child("span").cssClass("api-field-type").text(String.valueOf(errorCodeDefinition.getCode()));
            errorCode.child("span").cssClass("api-field-comment").text(StringEscapeUtils.escapeHtml4(errorCodeDefinition.getMessage()));
        }
    }

    private static void _appendEntityFieldTypeDefinition(HtmlBuilder type, FieldDefinition fieldDefinition, EngineDefinition engineDefinition) {
        String javaType = fieldDefinition.getJavaType();
        if (javaType.matches("List<.+>")) {
            String javaClassName = javaType.replaceFirst("^List<", "").replaceFirst(">$", "");
            if (_getModelDefinition(javaClassName, engineDefinition) != null) {
                type.child("span").text(StringEscapeUtils.escapeHtml4("List<"));
                type.child("a").attr("href", "#" + javaClassName).text(javaClassName);
                type.child("span").text(StringEscapeUtils.escapeHtml4(">"));
            } else {
                type.text(StringEscapeUtils.escapeHtml4(javaType));
            }
        } else if (_getModelDefinition(javaType, engineDefinition) != null) {
            type.child("a").attr("href", "#" + javaType).text(javaType);
        } else {
            type.text(javaType);
        }
    }

    private static EntityDefinition _getModelDefinition(String javaClassName, EngineDefinition engineDefinition) {
        if (engineDefinition.getModelDefinitions() == null) {
            return null;
        }
        for (EntityDefinition x : engineDefinition.getModelDefinitions()) {
            if (x.getJavaClassName().equals(javaClassName)) {
                return x;
            }
        }
        return null;
    }

    private static FieldDefinition _buildFieldDefinition(String comment, String name, String javaType, boolean required) {
        FieldDefinition fieldDefinition = new FieldDefinition();
        fieldDefinition.setComment(comment);
        fieldDefinition.setJavaType(javaType);
        fieldDefinition.setName(name);
        fieldDefinition.setRequired(required);
        return fieldDefinition;
    }

    private static String _buildCss() {
        StringBuffer sb = new StringBuffer("\n");
        sb.append("    .tail {\n");
        sb.append("          font-size: 14px;\n");
        sb.append("          text-align: center;\n");
        sb.append("    }\n");
        sb.append("    a:visited {\n");
        sb.append("          color: #333;\n");
        sb.append("    }\n");
        sb.append("    .api-name-bookmark {\n");
        sb.append("          color: #333;\n");
        sb.append("    }\n");
        sb.append("    .api-name {\n");
        sb.append("          color: #515151;\n");
        sb.append("    }\n");
        sb.append("    .api-field-name {\n");
        sb.append("          color: #00f;\n");
        sb.append("    }\n");
        sb.append("    .api-field-type {\n");
        sb.append("          color: green;\n");
        sb.append("    }\n");
        sb.append("    .api-field-required {\n");
        sb.append("          color: #853f26;\n");
        sb.append("    }\n");
        sb.append("    .api-field-comment {\n");
        sb.append("          color: gray;\n");
        sb.append("    }\n");
        sb.append("    .request-and-response-title {\n");
        sb.append("          color: #13C20F;\n");
        sb.append("    }\n");
        sb.append("    .alert {\n");
        sb.append("          padding: 15px;\n");
        sb.append("          margin-bottom: 20px;\n");
        sb.append("          border: 1px solid transparent;\n");
        sb.append("          border-radius: 4px;\n");
        sb.append("    }\n");
        sb.append("    .alert h4,p {\n");
        sb.append("          margin-top: 0;\n");
        sb.append("    }\n");
        sb.append("    .alert-success {\n");
        sb.append("          background-color: #dff0d8;\n");
        sb.append("          border-color: #d6e9c6;\n");
        sb.append("          color: #468847;\n");
        sb.append("    }\n");
        sb.append("    .alert-info {\n");
        sb.append("          background-color: #d9edf7;\n");
        sb.append("          border-color: #bce8f1;\n");
        sb.append("          color: #3a87ad;\n");
        sb.append("    }\n");
        sb.append("    .alert-warning {\n");
        sb.append("          background-color: #fcf8e3;\n");
        sb.append("          border-color: #fbeed5;\n");
        sb.append("          color: #c09853;\n");
        sb.append("    }\n");
        return sb.toString();
    }

    private static String _buildTime() {
        return new SimpleDateFormat("yyyy.MM.dd hh:mm:ss.SSS").format(System.currentTimeMillis());
    }
}
