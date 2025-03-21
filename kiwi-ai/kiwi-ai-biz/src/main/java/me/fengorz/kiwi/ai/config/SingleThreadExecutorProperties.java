package me.fengorz.kiwi.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "async.executor.single-thread")
public class SingleThreadExecutorProperties {

    private int corePoolSize = 1;
    private int maxPoolSize = 1;
    private int queueCapacity = 100;
    private String threadNamePrefix;
    private String rejectedExecutionPolicy;
    
}