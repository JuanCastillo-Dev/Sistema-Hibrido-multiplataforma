package com.clinica;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import java.util.Properties;

public class SshTunnelManager {
    private Session session;
    private int assignedLocalPort;

    public int openTunnel(ConfigManager config) throws Exception {
        String sshHost = config.get("ssh.host");
        int sshPort = config.getInt("ssh.port", 22);
        String sshUser = config.get("ssh.user");
        String pemPath = config.get("ssh.pem.path");
        int localPort = config.getInt("ssh.local.port", 3307);
        String remoteDbHost = config.get("db.remote.host");
        int remoteDbPort = config.getInt("db.remote.port", 3306);

        JSch jsch = new JSch();
        jsch.addIdentity(pemPath);

        session = jsch.getSession(sshUser, sshHost, sshPort);
        Properties sshConfig = new Properties();
        sshConfig.put("StrictHostKeyChecking", "no");
        session.setConfig(sshConfig);
        session.connect(15000);

        assignedLocalPort = session.setPortForwardingL(localPort, remoteDbHost, remoteDbPort);
        return assignedLocalPort;
    }

    public void closeTunnel() {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }
}