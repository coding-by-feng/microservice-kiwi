package me.fengorz.kiwi.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiModeProperties implements Serializable {

    private static final long serialVersionUID = -3233352570920497767L;

    private Map<String, String> mode;

}
