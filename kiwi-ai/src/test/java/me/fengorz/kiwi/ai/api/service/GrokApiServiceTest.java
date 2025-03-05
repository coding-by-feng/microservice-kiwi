package me.fengorz.kiwi.ai.api.service;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.AiApplication;
import me.fengorz.kiwi.common.sdk.constant.EnvConstants;
import me.fengorz.kiwi.test.EmbeddedRedisInitiator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Slf4j
@ActiveProfiles(EnvConstants.TEST)
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = AiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GrokApiServiceTest {

    @Autowired
    private GrokApiService service;

    @BeforeAll
    @Disabled
    public static void init() throws IOException {
        EmbeddedRedisInitiator.setUp();
    }

    @AfterAll
    @Disabled
    public static void tearDown() throws IOException {
        // Clean up resources here, if any
        EmbeddedRedisInitiator.tearDown();
    }

    @Test
    void call() {
        assertDoesNotThrow(() -> {
            String response = service.call("Test prompt for GrokApiService test.");
            log.info("Response of Grok AI is: {}", response);
        });
    }

}