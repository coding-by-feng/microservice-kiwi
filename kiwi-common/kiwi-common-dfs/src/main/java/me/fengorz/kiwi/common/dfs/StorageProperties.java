package me.fengorz.kiwi.common.dfs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds properties under prefix 'storage'. For example: storage.backend=ftp
 */
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {
    /** Backend type identifier, e.g. 'ftp', 'fastdfs'. */
    private String backend;

    public String getBackend() { return backend; }
    public void setBackend(String backend) { this.backend = backend; }
}

