package me.fengorz.kiwi.auth.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * Binds simple auth passcode from config server: kiwi.auth.simple.passcode
 */
@Getter
@Setter
@ToString
@RefreshScope
@Component
@ConfigurationProperties(prefix = "kiwi.auth.simple")
public class SimpleAuthProperties {
    /**
     * 6-digit numeric passcode used for simple verification.
     */
    private String passcode;
}

