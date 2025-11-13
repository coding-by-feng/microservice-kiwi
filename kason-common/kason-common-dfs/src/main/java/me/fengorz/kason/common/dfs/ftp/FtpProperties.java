package me.fengorz.kason.common.dfs.ftp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ftp")
public class FtpProperties {
    /** FTP hostname, e.g. kason-ftp */
    private String host = "kason-ftp";
    /** FTP port, default 21 */
    private int port = 21;
    /** Username; default anonymous */
    private String username = "anonymous";
    /** Password; default anonymous@ */
    private String password = "anonymous@";
    /** Base directory to store files under; must exist or be creatable. */
    private String baseDir = "/uploads";
    /** Use passive mode */
    private boolean passiveMode = true;
    /** Connect and data timeouts in milliseconds */
    private int connectTimeout = 15000;
    private int dataTimeout = 30000;
}

