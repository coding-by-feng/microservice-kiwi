package me.fengorz.kiwi.ai.service.openai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Data
@Component
@ConfigurationProperties(prefix = "ai.openai.api")
public class OpenaiApiProperties implements Serializable {

    private static final long serialVersionUID = -367572969083407339L;

    private String key;

    private String sttEndpoint;

    private String model;

}
