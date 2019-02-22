package com.github.microprograms.micro_api_sdk.utils;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.microprograms.micro_api_sdk.model.DeployDefinition;
import com.github.microprograms.micro_api_sdk.model.ApiServerDefinition;
import com.jcabi.ssh.Shell;
import com.jcabi.ssh.SshByPassword;

public class ApiDeployUtils {
	private static final Logger log = LoggerFactory.getLogger(ApiDeployUtils.class);

	public static void undeploy(ApiServerDefinition apiServerDefinition) throws IOException {
		String home = apiServerDefinition.getDeployDefinition().getRemoteJavaApplicationHome();
		remoteSsh(String.format("rm -rf %s", home), apiServerDefinition);
	}

	public static void deploy(ApiServerDefinition apiServerDefinition) throws IOException {
		deploySh(apiServerDefinition);
		deployLib(apiServerDefinition);
	}

	private static void deploySh(ApiServerDefinition apiServerDefinition) throws IOException {
		String home = apiServerDefinition.getDeployDefinition().getRemoteJavaApplicationHome();
		remoteSsh(String.format("mkdir -p %s/bin", home), apiServerDefinition);
		remoteSsh(String.format("echo -e \"%s\" > %s/bin/start.sh", _escapeEcho(_buildStartSh(apiServerDefinition)),
				home), apiServerDefinition);
		remoteSsh(
				String.format("echo -e \"%s\" > %s/bin/stop.sh", _escapeEcho(_buildStopSh(apiServerDefinition)), home),
				apiServerDefinition);
	}

	private static String _escapeEcho(String cmd) throws IOException {
		return cmd.replaceAll("\"", "\\\\\"").replaceAll("\n", "\\\\n").replaceAll("\\$!", "\\\\\\$!").replaceAll("`",
				"\\\\`");
	}

	private static String _buildStartSh(ApiServerDefinition apiServerDefinition) throws IOException {
		StringBuffer sb = new StringBuffer();
		sb.append("#!/bin/bash").append("\n\n");
		String home = apiServerDefinition.getDeployDefinition().getRemoteJavaApplicationHome();
		String port = String.valueOf(apiServerDefinition.getServerAddressDefinition().getPort());
		String packages = apiServerDefinition.getJavaPackageName();
		String classpath = String.format("%s/lib/*", home);
		String outfile = String.format("%s/server.nohup.out", home);
		String main = "com.github.microprograms.micro_http_server_runtime.MicroHttpServer";
		sb.append(String.format("nohup java -cp \"%s\" %s -p %s -s %s > %s 2>&1 &", classpath, main, port, packages,
				outfile)).append("\n");
		sb.append(String.format("echo $! > %s/bin/server.pid", home));
		return sb.toString();
	}

	private static String _buildStopSh(ApiServerDefinition apiServerDefinition) throws IOException {
		StringBuffer sb = new StringBuffer();
		sb.append("#!/bin/bash").append("\n\n");
		String home = apiServerDefinition.getDeployDefinition().getRemoteJavaApplicationHome();
		sb.append(String.format("kill `cat %s/bin/server.pid`", home)).append("\n");
		sb.append(String.format("rm -rf %s/bin/server.pid", home));
		return sb.toString();
	}

	private static void deployLib(ApiServerDefinition apiServerDefinition) throws IOException {
		String isExistSetenvSh = localSsh("[[ -f ~/.micro_api_sdk/setenv.sh ]]; echo $?;", apiServerDefinition);
		if (Integer.valueOf(isExistSetenvSh.replaceAll("\\s*", "")) != 0) {
			throw new RuntimeException("canot found \"~/.micro_api_sdk/setenv.sh\" file, Maven may not work properly.");
		}
		String localHome = apiServerDefinition.getDeployDefinition().getLocalMavenProjectHome();
		String remoteHome = apiServerDefinition.getDeployDefinition().getRemoteJavaApplicationHome();
		String remoteHost = apiServerDefinition.getServerAddressDefinition().getHost();
		String cd = String.format("cd %s;", localHome);
		String setenv = "source ~/.micro_api_sdk/setenv.sh;";
		String jar = "mvn clean package; mvn dependency:copy-dependencies -DoutputDirectory=lib; cp target/*.jar lib/;";
		String rsync = String.format(
				"export RSYNC_PASSWORD=pass; rsync -vzrtopg --delete --progress lib/ common@%s::common/%s/lib/;",
				remoteHost, remoteHome);
		String del = "rm -rf lib/;";
		localSsh(StringUtils.join(Arrays.asList(cd, setenv, jar, rsync, del), " "), apiServerDefinition);
	}

	public static void restart(ApiServerDefinition apiServerDefinition) throws IOException {
		stop(apiServerDefinition);
		start(apiServerDefinition);
	}

	public static void stop(ApiServerDefinition apiServerDefinition) throws IOException {
		String home = apiServerDefinition.getDeployDefinition().getRemoteJavaApplicationHome();
		remoteSsh(String.format("sudo sh %s/bin/stop.sh", home), apiServerDefinition);
	}

	public static void start(ApiServerDefinition apiServerDefinition) throws IOException {
		String home = apiServerDefinition.getDeployDefinition().getRemoteJavaApplicationHome();
		remoteSsh(String.format("sudo sh %s/bin/start.sh", home), apiServerDefinition);
	}

	public static void showLatestServerOut(int lines, ApiServerDefinition apiServerDefinition) throws IOException {
		String home = apiServerDefinition.getDeployDefinition().getRemoteJavaApplicationHome();
		remoteSsh(String.format("sudo tail -%d %s/server.nohup.out", lines, home), apiServerDefinition);
	}

	public static void showLatestServerOut(ApiServerDefinition apiServerDefinition) throws IOException {
		showLatestServerOut(100, apiServerDefinition);
	}

	public static String remoteSsh(String cmd, ApiServerDefinition apiServerDefinition) throws IOException {
		DeployDefinition deployDefinition = apiServerDefinition.getDeployDefinition();
		Shell shell = new SshByPassword(apiServerDefinition.getServerAddressDefinition().getHost(),
				deployDefinition.getRemoteSshPort(), deployDefinition.getRemoteSshUser(),
				deployDefinition.getRemoteSshPassword());
		String stdout = new Shell.Plain(shell).exec(cmd);
		log.info("remoteSsh> {}{}", cmd, StringUtils.isBlank(stdout) ? "" : "\n" + stdout);
		return stdout;
	}

	public static String localSsh(String cmd, ApiServerDefinition apiServerDefinition) throws IOException {
		DeployDefinition deployDefinition = apiServerDefinition.getDeployDefinition();
		Shell shell = new SshByPassword("localhost", deployDefinition.getLocalSshPort(),
				deployDefinition.getLocalSshUser(), deployDefinition.getLocalSshPassword());
		String stdout = new Shell.Plain(shell).exec(cmd);
		log.info("localSsh> {}{}", cmd, StringUtils.isBlank(stdout) ? "" : "\n" + stdout);
		return stdout;
	}
}
