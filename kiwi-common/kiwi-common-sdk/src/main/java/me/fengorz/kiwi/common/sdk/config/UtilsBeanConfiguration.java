package me.fengorz.kiwi.common.sdk.config;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.ApplicationContext;

import me.fengorz.kiwi.common.sdk.util.spring.SpringUtils;

@Configurable
public class UtilsBeanConfiguration {
    public UtilsBeanConfiguration(ApplicationContext applicationContext) {
        SpringUtils.init(applicationContext);
    }
}
