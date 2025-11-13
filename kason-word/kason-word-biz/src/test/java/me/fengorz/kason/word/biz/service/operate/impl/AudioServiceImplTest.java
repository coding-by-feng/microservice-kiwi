package me.fengorz.kason.word.biz.service.operate.impl;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kason.common.sdk.constant.EnvConstants;
import me.fengorz.kason.word.biz.WordBizTestApplication;
import me.fengorz.kason.word.biz.service.operate.AudioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Slf4j
@ActiveProfiles({EnvConstants.TEST})
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = WordBizTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AudioServiceImplTest {

    @Autowired
    private AudioService audioService;

    @Test
    @SneakyThrows
    void generateEnglishVoice() {
        String longEnglishSentence = "The quick brown fox jumps over the lazy dog because it wants to catch the squirrel on the other side of the fence, and it needs to do it before the sun sets behind the mountains.";
        String uploadResult = audioService.generateEnglishVoice(longEnglishSentence);
        log.info("uploadResult: {}", uploadResult);
    }

    @Test
    @SneakyThrows
    void generateChineseVoice() {
        String longChineseSentence = "希望是美好的事情，也许是最美好的事情";
        String uploadResult = audioService.generateChineseVoice(longChineseSentence);
        log.info("uploadResult: {}", uploadResult);
    }

}