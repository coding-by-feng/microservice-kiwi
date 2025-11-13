package me.fengorz.kason.ai.service.grok;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Data
@Component
@ConfigurationProperties(prefix = "ai.grok.api")
public class GrokApiProperties implements Serializable {

    private static final long serialVersionUID = -367572969083407339L;

    private String key;

    private String endpoint;

    private String model;

    private Integer threadPromptsLineSize = 50;

    private Integer threadPoolSize;

    private Integer threadTimeoutSecs;

}
