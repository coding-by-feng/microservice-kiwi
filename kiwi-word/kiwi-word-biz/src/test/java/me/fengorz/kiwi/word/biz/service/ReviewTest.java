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

package me.fengorz.kiwi.word.biz.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Order;
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
import me.fengorz.kiwi.admin.api.dto.UserFullInfoDTO;
import me.fengorz.kiwi.admin.api.feign.UserApi;
import me.fengorz.kiwi.bdf.core.service.SeqService;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.fastdfs.service.DfsService;
import me.fengorz.kiwi.common.sdk.constant.EnvConstants;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.util.json.KiwiJsonUtils;
import me.fengorz.kiwi.word.api.common.WordConstants;
import me.fengorz.kiwi.word.api.common.enumeration.ReviewAudioTypeEnum;
import me.fengorz.kiwi.word.api.common.enumeration.ReviewDailyCounterTypeEnum;
import me.fengorz.kiwi.word.api.entity.ParaphraseStarListDO;
import me.fengorz.kiwi.word.api.entity.WordReviewAudioDO;
import me.fengorz.kiwi.word.api.vo.ParaphraseStarListVO;
import me.fengorz.kiwi.word.api.vo.WordReviewDailyCounterVO;
import me.fengorz.kiwi.word.biz.WordBizApplication;
import me.fengorz.kiwi.word.biz.service.base.ParaphraseService;
import me.fengorz.kiwi.word.biz.service.base.ParaphraseStarListService;
import me.fengorz.kiwi.word.biz.service.base.WordMainService;
import me.fengorz.kiwi.word.biz.service.operate.ReviewService;
import me.fengorz.kiwi.word.biz.util.WordDataSetupUtils;

@Slf4j
@ActiveProfiles({EnvConstants.DEV, EnvConstants.BASE})
@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:env.properties")
@SpringBootTest(classes = WordBizApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ReviewTest {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private WordMainService wordMainService;

    @Autowired
    private ParaphraseService paraphraseService;

    @Autowired
    private DfsService dfsService;

    @Autowired
    private ParaphraseStarListService paraphraseStarListService;

    @Autowired
    private SeqService seqService;

    @Autowired
    private UserApi userApi;

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
        Assertions.assertDoesNotThrow(() -> reviewService.generateTtsVoice());
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
    @Order(1)
    // @Disabled
    void findWordReviewAudio() {

        // List<ParaphraseDO> test = paraphraseService.listByWordName("test");
        // Assertions.assertNotNull(test);
        // ParaphraseDO paraphraseDO = test.get(0);
        // WordReviewAudioDO wordReviewAudio = reviewService.findWordReviewAudio(paraphraseDO.getParaphraseId(),
        // ReviewAudioTypeEnum.PARAPHRASE_EN.getType());

        // id=3749976, sourceId=2774291, type=0
        WordReviewAudioDO wordReviewAudio =
            reviewService.findWordReviewAudio(2774291, ReviewAudioTypeEnum.WORD_SPELLING.getType());
        Assertions.assertNotNull(wordReviewAudio);
        Assertions.assertEquals(3749976, wordReviewAudio.getId());
        byte[] bytes = this.dfsService.downloadFile(wordReviewAudio.getGroupName(), wordReviewAudio.getFilePath());
        FileUtil.writeBytes(bytes, "/Users/zhanshifeng/Documents/temp/test_paraphrase_2774291_0.mp3");
    }

    @SneakyThrows
    @Test
    @Disabled
    void test_findWordReviewAudio() {
        WordReviewAudioDO wordReviewAudio =
            reviewService.findWordReviewAudio(2774367, ReviewAudioTypeEnum.COMBO.getType());
        byte[] bytes = this.dfsService.downloadFile(wordReviewAudio.getGroupName(), wordReviewAudio.getFilePath());
        FileUtil.writeBytes(bytes, "/Users/zhanshifeng/Documents/temp/test_all.mp3");
    }

    @Test
    @Disabled
    void test_createCommonParaphraseCollection() {
        R<?> echo = userApi.info("echo");
        Assertions.assertFalse(echo.getData() instanceof String);
        log.info("echo.getData(): {}", echo.getData());
        UserFullInfoDTO echoUserInfo = (UserFullInfoDTO)echo.getData();
        Integer seqId = seqService.genCommonIntSequence();
        ParaphraseStarListDO paraphraseStarListDO =
            new ParaphraseStarListDO().setId(seqId).setOwner(echoUserInfo.getSysUser().getUserId())
                .setCreateTime(LocalDateTime.now()).setIsDel(GlobalConstants.FLAG_N).setRemark("auto gen.")
                .setListName(WordConstants.COMMON_PARAPHRASE_COLLECTION.IELTS);
        paraphraseStarListService.save(paraphraseStarListDO);
        Assertions.assertNotNull(paraphraseStarListService.getById(seqId));
    }

    @Test
    @Disabled
    void test_commonParaphraseCollection() {
        R<UserFullInfoDTO> echo = userApi.info("echo");
        log.info("echo.getData(): {}", echo.getData());
        UserFullInfoDTO echoUserInfo = echo.getData();
        Assertions.assertNotNull(echoUserInfo);
        log.info("echoUserInfo userid: {}", echoUserInfo.getSysUser().getUserId());
        List<ParaphraseStarListVO> list =
            paraphraseStarListService.getCurrentUserList(echoUserInfo.getSysUser().getUserId());
        Assertions.assertNotNull(list);
        Assertions.assertTrue(list.size() > 0);
        List<String> collectionNames = list.stream().map(collection -> {
            log.info("Collection name is: {}", collection.getListName());
            return collection.getListName();
        }).collect(Collectors.toList());
        Assertions.assertTrue(collectionNames.contains(WordConstants.COMMON_PARAPHRASE_COLLECTION.IELTS));
    }

    @Test
    @Disabled
    @SneakyThrows
    void test_IELTS_wordList() {
        String resourcePath = this.getClass().getResource(GlobalConstants.SYMBOL_FORWARD_SLASH).getPath();
        Files.list(Paths.get(resourcePath + "/word-list")).forEach(path -> {
            log.info("path: {}", path);
            try {
                WordDataSetupUtils.extractWordList(path.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    @Order(2)
    @Disabled
    void test_evictWordReviewAudio() {
        reviewService.evictWordReviewAudio(2774291, ReviewAudioTypeEnum.WORD_SPELLING.getType());
    }

}