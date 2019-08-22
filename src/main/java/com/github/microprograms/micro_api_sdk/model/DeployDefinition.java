package com.github.microprograms.micro_api_sdk.model;

import java.io.Serializable;

public class DeployDefinition implements Serializable {
	private static final long serialVersionUID = 1L;

    private String localMavenProjectHome;
    private int localSshPort;
    private String localSshUser;
    private String localSshPassword;
    private String remoteJavaApplicationHome;
    private int remoteSshPort;
    private String remoteSshUser;
    private String remoteSshPassword;

    public String getLocalMavenProjectHome() {
        return localMavenProjectHome;
    }

    public void setLocalMavenProjectHome(String localMavenProjectHome) {
        this.localMavenProjectHome = localMavenProjectHome;
    }

    public int getLocalSshPort() {
        return localSshPort;
    }

    public void setLocalSshPort(int localSshPort) {
        this.localSshPort = localSshPort;
    }

    public String getLocalSshUser() {
        return localSshUser;
    }

    public void setLocalSshUser(String localSshUser) {
        this.localSshUser = localSshUser;
    }

    public String getLocalSshPassword() {
        return localSshPassword;
    }

    public void setLocalSshPassword(String localSshPassword) {
        this.localSshPassword = localSshPassword;
    }

    public String getRemoteJavaApplicationHome() {
        return remoteJavaApplicationHome;
    }

    public void setRemoteJavaApplicationHome(String remoteJavaApplicationHome) {
        this.remoteJavaApplicationHome = remoteJavaApplicationHome;
    }

    public int getRemoteSshPort() {
        return remoteSshPort;
    }

    public void setRemoteSshPort(int remoteSshPort) {
        this.remoteSshPort = remoteSshPort;
    }

    public String getRemoteSshUser() {
        return remoteSshUser;
    }

    public void setRemoteSshUser(String remoteSshUser) {
        this.remoteSshUser = remoteSshUser;
    }

    public String getRemoteSshPassword() {
        return remoteSshPassword;
    }

    public void setRemoteSshPassword(String remoteSshPassword) {
        this.remoteSshPassword = remoteSshPassword;
    }
}
