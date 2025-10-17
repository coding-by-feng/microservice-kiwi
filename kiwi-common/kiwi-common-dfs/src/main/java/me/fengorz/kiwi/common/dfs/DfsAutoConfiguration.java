package me.fengorz.kiwi.common.dfs;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DfsAutoConfiguration {

    @Bean(name = "dfsService")
    @Primary
    @ConditionalOnBean(name = "fastDfsService")
    public DfsService dfsServiceFast(@Qualifier("fastDfsService") DfsService impl) {
        return impl;
    }

    @Bean(name = "dfsService")
    @Primary
    @ConditionalOnBean(name = "ftpDfsService")
    public DfsService dfsServiceFtp(@Qualifier("ftpDfsService") DfsService impl) {
        return impl;
    }
}

