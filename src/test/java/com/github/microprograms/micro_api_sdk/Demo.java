package com.github.microprograms.micro_api_sdk;

import java.io.IOException;

import com.github.microprograms.micro_api_sdk.model.EngineDefinition;
import com.github.microprograms.micro_api_sdk.utils.ApiDocumentUtils;
import com.github.microprograms.micro_api_sdk.utils.ApiEngineGeneratorUtils;

public class Demo {
    public static void main(String[] args) throws IOException {
        String srcFolder = "src/test/java";
        EngineDefinition engineDefinition = ApiEngineGeneratorUtils.buildEngineDefinition("src/test/java/api-engine.json");
        ApiEngineGeneratorUtils.deleteModelJavaFiles(srcFolder, engineDefinition);
        ApiEngineGeneratorUtils.createModelJavaFiles(srcFolder, engineDefinition);
        ApiEngineGeneratorUtils.updateApiJavaFiles(srcFolder, engineDefinition);
        ApiEngineGeneratorUtils.deleteUnusedApiJavaFiles(srcFolder, engineDefinition);
        ApiEngineGeneratorUtils.updateErrorCodeJavaFile(srcFolder, engineDefinition);
        ApiDocumentUtils.writeApiHtmlDocumentFile("src/test/java", engineDefinition);
    }
}
