package com.github.microprograms.micro_api_sdk.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;

import com.github.javaparser.ast.CompilationUnit;

public class JavaParserUtils {

    public static File buildJavaSourceFile(String srcFolder, String javaPackageName, String javaClassName) {
        return new File(srcFolder + File.separator + javaPackageName.replaceAll("\\.", File.separator) + File.separator + javaClassName + ".java");
    }

    public static void write(CompilationUnit cu, File javaSourceFile, Charset encoding) throws IOException {
        OutputStream output = new FileOutputStream(javaSourceFile);
        try {
            IOUtils.write(cu.toString(), output, encoding);
        } finally {
            IOUtils.closeQuietly(output);
        }
    }
}
