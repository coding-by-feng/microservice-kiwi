package me.fengorz.kiwi.tools.config;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.bdf.config.CoreConfig;
import me.fengorz.kiwi.common.cache.redis.config.CacheConfig;
import me.fengorz.kiwi.common.db.config.DbConfig;
import me.fengorz.kiwi.common.dfs.DfsConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Tools service base configuration ensuring core, DB and cache configs are loaded.
 */
@Slf4j
@Configuration
@ComponentScan(basePackages = "me.fengorz.kiwi.**")
@Import({CoreConfig.class, DbConfig.class, DfsConfig.class})
public class ToolsConfig {

    public ToolsConfig() {
        log.info("ToolsConfig initialized (core/db configs imported).");
    }

    // Import CacheConfig only if ms.config.exclude-cache=false (default true for tests to disable)
    @Configuration
    @ConditionalOnProperty(prefix = "ms.config", name = "exclude-cache", havingValue = "false", matchIfMissing = true)
    @Import(CacheConfig.class)
    static class ConditionalCacheImport {
        ConditionalCacheImport() {
            log.info("CacheConfig imported (ms.config.exclude-cache=false).");
        }
    }
}