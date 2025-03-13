package me.fengorz.kiwi.ai.service;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.AiApplication;
import me.fengorz.kiwi.ai.grok.service.GrokAiService;
import me.fengorz.kiwi.common.sdk.constant.EnvConstants;
import me.fengorz.kiwi.common.sdk.enumeration.AiPromptModeEnum;
import me.fengorz.kiwi.common.sdk.enumeration.LanguageEnum;
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
@Disabled
class GrokAiChatServiceTest {

    @Autowired
    private GrokAiService service;

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
            log.info("{} response of Grok AI is: {}", AiPromptModeEnum.DIRECTLY_TRANSLATION.name(),
                    service.call("Hope is a good thing, maybe the best of things!",
                            AiPromptModeEnum.DIRECTLY_TRANSLATION, LanguageEnum.ZH_CN));
            log.info("{} response of Grok AI is: {}", AiPromptModeEnum.TRANSLATION_AND_EXPLANATION.name(),
                    service.call("希望是一件好事，也许是最好的事!",
                            AiPromptModeEnum.TRANSLATION_AND_EXPLANATION, LanguageEnum.EN));
        });
    }

}