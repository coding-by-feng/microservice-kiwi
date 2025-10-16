package me.fengorz.kiwi.tools.config;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.bdf.config.CoreConfig;
import me.fengorz.kiwi.common.cache.redis.config.CacheConfig;
import me.fengorz.kiwi.common.db.config.DbConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Tools service base configuration ensuring core, DB and cache configs are loaded.
 * (Mirrors the pattern used in AiConfig.)
 */
@Slf4j
@Configuration
@ComponentScan(basePackages = "me.fengorz.kiwi.**")
@Import({CoreConfig.class, DbConfig.class, CacheConfig.class})
public class ToolsConfig {

    public ToolsConfig() {
        log.info("ToolsConfig initialized (core/db/cache configs imported).");
    }

}




