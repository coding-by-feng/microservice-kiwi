package me.fengorz.kiwi.ai.api.model;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 04/03/2025 3:52 PM
 */
@Data
@Component
public class GrokApiModel implements Serializable {

    private static final long serialVersionUID = -367572969083407339L;

    @Value("${grok.api.key}")
    private String grokApiKey;

    @Value("${grok.api.endpoint}")
    private String grokApiEndpoint;

}
