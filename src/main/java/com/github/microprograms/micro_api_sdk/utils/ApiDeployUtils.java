package com.github.microprograms.micro_api_sdk.utils;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.microprograms.micro_api_sdk.model.DeployDefinition;
import com.github.microprograms.micro_api_sdk.model.EngineDefinition;
import com.jcabi.ssh.Shell;
import com.jcabi.ssh.SshByPassword;

public class ApiDeployUtils {
    private static final Logger log = LoggerFactory.getLogger(ApiDeployUtils.class);

    public static void undeploy(EngineDefinition engineDefinition) throws IOException {
        String home = engineDefinition.getDeployDefinition().getRemoteJavaApplicationHome();
        remoteSsh(String.format("rm -rf %s", home), engineDefinition);
    }

    public static void deploy(EngineDefinition engineDefinition) throws IOException {
        deploySh(engineDefinition);
        deployLib(engineDefinition);
    }

    private static void deploySh(EngineDefinition engineDefinition) throws IOException {
        String home = engineDefinition.getDeployDefinition().getRemoteJavaApplicationHome();
        remoteSsh(String.format("mkdir -p %s/bin", home), engineDefinition);
        remoteSsh(String.format("echo -e \"%s\" > %s/bin/start.sh", _buildStartSh(engineDefinition).replaceAll("\"", "\\\\\"").replaceAll("\\$!", "\\\\\\$!"), home), engineDefinition);
        remoteSsh(String.format("echo -e \"%s\" > %s/bin/stop.sh", _buildStopSh(engineDefinition).replaceAll("`", "\\\\`"), home), engineDefinition);
    }

    private static String _buildStartSh(EngineDefinition engineDefinition) throws IOException {
        StringBuffer sb = new StringBuffer();
        sb.append("#!/bin/bash").append("\n\n");
        String home = engineDefinition.getDeployDefinition().getRemoteJavaApplicationHome();
        String port = String.valueOf(engineDefinition.getServerAddressDefinition().getPort());
        String packages = engineDefinition.getJavaPackageName();
        String classpath = String.format("%s/lib/*", home);
        String outfile = String.format("%s/server.nohup.out", home);
        String main = "com.github.microprograms.micro_http_server_runtime.MicroHttpServer";
        sb.append(String.format("nohup java -cp \"%s\" %s -p %s -s %s > %s 2>&1 &", classpath, main, port, packages, outfile)).append("\n");
        sb.append(String.format("echo $! > %s/bin/server.pid", home));
        return sb.toString();
    }

    private static String _buildStopSh(EngineDefinition engineDefinition) throws IOException {
        StringBuffer sb = new StringBuffer();
        sb.append("#!/bin/bash").append("\n\n");
        String home = engineDefinition.getDeployDefinition().getRemoteJavaApplicationHome();
        sb.append(String.format("kill `cat %s/bin/server.pid`", home)).append("\n");
        sb.append(String.format("rm -rf %s/bin/server.pid", home));
        return sb.toString();
    }

    private static void deployLib(EngineDefinition engineDefinition) throws IOException {
        String home = engineDefinition.getDeployDefinition().getLocalMavenProjectHome();
        String cd = String.format("cd %s;", home);
        String jar = "export PATH=~/java/apache-maven-3.3.9/bin:$PATH; mvn clean package; mvn dependency:copy-dependencies -DoutputDirectory=lib; cp target/*.jar lib/;";
        String rsync = String.format("export RSYNC_PASSWORD=pass; rsync -vzrtopg --delete --progress lib/ microprograms@%s::microprograms/test/lib/;", engineDefinition.getServerAddressDefinition().getHost());
        String del = "rm -rf lib/;";
        localSsh(cd + jar + rsync + del, engineDefinition);
    }

    public static void restart(EngineDefinition engineDefinition) throws IOException {
        stop(engineDefinition);
        start(engineDefinition);
    }

    public static void stop(EngineDefinition engineDefinition) throws IOException {
        String home = engineDefinition.getDeployDefinition().getRemoteJavaApplicationHome();
        remoteSsh(String.format("sudo sh %s/bin/stop.sh", home), engineDefinition);
    }

    public static void start(EngineDefinition engineDefinition) throws IOException {
        String home = engineDefinition.getDeployDefinition().getRemoteJavaApplicationHome();
        remoteSsh(String.format("sudo sh %s/bin/start.sh", home), engineDefinition);
    }

    public static void remoteSsh(String cmd, EngineDefinition engineDefinition) throws IOException {
        DeployDefinition deployDefinition = engineDefinition.getDeployDefinition();
        Shell shell = new SshByPassword(engineDefinition.getServerAddressDefinition().getHost(), deployDefinition.getRemoteSshPort(), deployDefinition.getRemoteSshUser(), deployDefinition.getRemoteSshPassword());
        String stdout = new Shell.Plain(shell).exec(cmd);
        log.info("remoteSsh> " + stdout);
    }

    public static void localSsh(String cmd, EngineDefinition engineDefinition) throws IOException {
        DeployDefinition deployDefinition = engineDefinition.getDeployDefinition();
        Shell shell = new SshByPassword("localhost", deployDefinition.getLocalSshPort(), deployDefinition.getLocalSshUser(), deployDefinition.getLocalSshPassword());
        String stdout = new Shell.Plain(shell).exec(cmd);
        log.info("localSsh> " + stdout);
    }

    public static void main(String[] args) throws Exception {
        Shell shell = new SshByPassword("localhost", 22, "xuzewei", "xzw");
        Shell.Plain plain = new Shell.Plain(shell);
        System.out.println(plain.exec("export PATH=~/java/apache-maven-3.3.9/bin:$PATH; mvn"));
    }
}
