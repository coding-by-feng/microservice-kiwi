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

package me.fengorz.kiwi.word.biz.service.operate.impl;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import cn.hutool.core.io.FileUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.fastdfs.service.DfsService;
import me.fengorz.kiwi.common.sdk.constant.EnvConstants;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.util.json.KiwiJsonUtils;
import me.fengorz.kiwi.word.api.common.enumeration.ReviewAudioTypeEnum;
import me.fengorz.kiwi.word.api.common.enumeration.ReviewDailyCounterTypeEnum;
import me.fengorz.kiwi.word.api.entity.ParaphraseDO;
import me.fengorz.kiwi.word.api.entity.WordReviewAudioDO;
import me.fengorz.kiwi.word.api.vo.WordReviewDailyCounterVO;
import me.fengorz.kiwi.word.biz.WordBizApplication;
import me.fengorz.kiwi.word.biz.service.base.ParaphraseService;
import me.fengorz.kiwi.word.biz.service.base.WordMainService;
import me.fengorz.kiwi.word.biz.service.operate.ReviewService;

@Slf4j
@ActiveProfiles({EnvConstants.DEV, EnvConstants.BASE})
@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:env.properties")
@SpringBootTest(classes = WordBizApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ReviewServiceImplTest {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private WordMainService wordMainService;

    @Autowired
    private ParaphraseService paraphraseService;

    @Autowired
    private DfsService dfsService;

    @Test
    @Disabled
    void initPermanent() {
        Assertions.assertDoesNotThrow(() -> reviewService.initPermanent(false, false));
    }

    @Test
    @Disabled
    void generateTtsVoiceFromParaphraseId() {
        Assertions.assertDoesNotThrow(() -> reviewService.generateTtsVoiceFromParaphraseId(2774367));
    }

    @Test
    @Disabled
    void generateTtsVoice() {
        Assertions.assertDoesNotThrow(() -> reviewService.generateTtsVoice(true));
    }

    @Test
    @Disabled
    void createTheDays() {
        Assertions.assertDoesNotThrow(() -> reviewService.createTheDays(1));
    }

    @Test
    @Disabled
    void listReviewCounterVO() {
        List<WordReviewDailyCounterVO> list = reviewService.listReviewCounterVO(1);
        log.info("listReviewCounterVO >>>>>>>>>>>> {}", KiwiJsonUtils.toJsonStr(list));
        Assertions.assertEquals(ReviewDailyCounterTypeEnum.values().length, list.size());
    }

    @Test
    @Disabled
    void testReplace() {
        System.out.println("--------------> AA...BB".replaceAll("\\.\\.\\.", GlobalConstants.WHAT));
    }

    @SneakyThrows
    @Test
    @Disabled
    void findWordReviewAudio() {
        List<ParaphraseDO> test = paraphraseService.listByWordName("test");
        Assertions.assertNotNull(test);
        ParaphraseDO paraphraseDO = test.get(0);
        WordReviewAudioDO wordReviewAudio = reviewService.findWordReviewAudio(paraphraseDO.getParaphraseId(),
            ReviewAudioTypeEnum.PARAPHRASE_EN.getType());
        byte[] bytes = this.dfsService.downloadFile(wordReviewAudio.getGroupName(), wordReviewAudio.getFilePath());
        FileUtil.writeBytes(bytes, "test_paraphrase.mp3");
    }

    @SneakyThrows
    @Test
    // @Disabled
    void test_findWordReviewAudio() {
        WordReviewAudioDO wordReviewAudio = reviewService.findWordReviewAudio(1510384, ReviewAudioTypeEnum.NON_REVIEW_SPELL.getType());
        byte[] bytes = this.dfsService.downloadFile(wordReviewAudio.getGroupName(), wordReviewAudio.getFilePath());
        FileUtil.writeBytes(bytes, "test_paraphrase_ch.mp3");
    }
}