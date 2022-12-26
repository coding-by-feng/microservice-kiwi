/*
 *
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 *
 */

package me.fengorz.kiwi.word.biz.service.base.impl;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.constant.EnvConstants;
import me.fengorz.kiwi.common.tts.service.BaiduTtsService;
import me.fengorz.kiwi.word.api.entity.WordReviewAudioDO;
import me.fengorz.kiwi.word.biz.WordBizApplication;
import me.fengorz.kiwi.word.biz.service.base.ParaphraseStarListService;
import me.fengorz.kiwi.word.biz.service.base.ParaphraseStarRelService;
import me.fengorz.kiwi.word.biz.service.base.ReviewAudioService;
import me.fengorz.kiwi.word.biz.service.operate.AudioService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.util.List;

import static me.fengorz.kiwi.common.tts.TtsConstants.BEAN_NAMES.BAIDU_TTS_SERVICE_IMPL;

@Slf4j
@ActiveProfiles({EnvConstants.DEV, EnvConstants.BASE})
@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:env.properties")
@SpringBootTest(classes = WordBizApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BaseServiceTest {

    @Autowired
    private ParaphraseStarRelService paraphraseStarRelService;
    @Autowired
    private ParaphraseStarListService starListService;
    @Autowired
    private AudioService audioService;
    @Autowired
    @Qualifier(BAIDU_TTS_SERVICE_IMPL)
    private BaiduTtsService baiduTtsService;
    @Autowired
    private ReviewAudioService reviewAudioService;

    @Test
    @Disabled
    public void test_listNotGeneratedVoice() {
        Assertions.assertTrue(CollectionUtils.isEmpty(paraphraseStarRelService.listNotGeneratedVoice()));
        List<Integer> notAllGeneratedList = paraphraseStarRelService.listNotAllGeneratedVoice();
        Assertions.assertTrue(CollectionUtils.isNotEmpty(notAllGeneratedList));
        log.info("notAllGeneratedList size={}", notAllGeneratedList.size());
    }

    @Test
    @Disabled
    public void test_rememberOne() {
        Assertions.assertDoesNotThrow(() -> {
            starListService.rememberOne(2539690, 1266094);
        });
    }

    @SneakyThrows
    @Disabled
    @Test
    public void test_baidu_tts() {
        byte[] bytes = baiduTtsService.speech("测试一下");
        FileUtils.writeByteArrayToFile(new File("baidu-tts-test.mp3"), bytes);
    }

    @Test
    // @Disabled
    public void test_listIncorrectAudioByVoicerss() {
        List<WordReviewAudioDO> result = reviewAudioService.listIncorrectAudioByVoicerss();
        Assertions.assertTrue(CollectionUtils.isNotEmpty(result));
        log.info("result size={}", result.size());
    }

}