package me.fengorz.kiwi.ai;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.test.config.TestSecurityConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @Author Kason Zhan
 * @Date 06/03/2025
 */
@Slf4j
@Configuration
@Import({TestSecurityConfig.class})
public class AiTestConfig {

    public AiTestConfig() {
        log.info("AiTestConfig...");
    }

}
