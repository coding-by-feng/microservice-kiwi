package me.fengorz.kason.common.sdk.config;

import me.fengorz.kason.common.sdk.util.spring.SpringUtils;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.ApplicationContext;

@Configurable
public class UtilsBeanConfiguration {
    public UtilsBeanConfiguration(ApplicationContext applicationContext) {
        SpringUtils.init(applicationContext);
    }
}
