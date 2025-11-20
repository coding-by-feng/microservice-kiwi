package me.fengorz.kiwi.common.dfs.ftp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ftp")
public class FtpProperties {
    private String host;
    private int port = 21;
    private String username;
    private String password;
    private String baseDir;
    private boolean passiveMode = true;
    private int connectTimeout = 15000;
    private int dataTimeout = 30000;

    public FtpProperties() {}

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getBaseDir() { return baseDir; }
    public void setBaseDir(String baseDir) { this.baseDir = baseDir; }
    public boolean isPassiveMode() { return passiveMode; }
    public void setPassiveMode(boolean passiveMode) { this.passiveMode = passiveMode; }
    public int getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(int connectTimeout) { this.connectTimeout = connectTimeout; }
    public int getDataTimeout() { return dataTimeout; }
    public void setDataTimeout(int dataTimeout) { this.dataTimeout = dataTimeout; }
}
