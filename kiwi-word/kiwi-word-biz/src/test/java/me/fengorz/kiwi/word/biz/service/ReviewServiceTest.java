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

import cn.hutool.core.io.FileUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.ApiContants;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.db.service.SeqService;
import me.fengorz.kiwi.common.dfs.DfsService;
import me.fengorz.kiwi.common.sdk.constant.EnvConstants;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.util.json.KiwiJsonUtils;
import me.fengorz.kiwi.common.tts.service.BaiduTtsService;
import me.fengorz.kiwi.upms.api.dto.UserFullInfoDTO;
import me.fengorz.kiwi.upms.api.feign.UserApi;
import me.fengorz.kiwi.word.api.common.WordConstants;
import me.fengorz.kiwi.word.api.common.enumeration.ReviseAudioTypeEnum;
import me.fengorz.kiwi.word.api.common.enumeration.ReviseDailyCounterTypeEnum;
import me.fengorz.kiwi.word.api.entity.ParaphraseStarListDO;
import me.fengorz.kiwi.word.api.entity.WordReviewAudioDO;
import me.fengorz.kiwi.word.api.vo.ParaphraseStarListVO;
import me.fengorz.kiwi.word.api.vo.WordReviewDailyCounterVO;
import me.fengorz.kiwi.word.api.vo.detail.ParaphraseVO;
import me.fengorz.kiwi.word.api.vo.detail.WordQueryVO;
import me.fengorz.kiwi.word.biz.WordBizApplication;
import me.fengorz.kiwi.word.biz.service.base.ParaphraseService;
import me.fengorz.kiwi.word.biz.service.base.ParaphraseStarListService;
import me.fengorz.kiwi.word.biz.service.base.WordMainService;
import me.fengorz.kiwi.word.biz.service.initialing.RevisePermanentAudioHelper;
import me.fengorz.kiwi.word.biz.service.operate.OperateService;
import me.fengorz.kiwi.word.biz.service.operate.ReviewService;
import me.fengorz.kiwi.word.biz.util.WordDataSetupUtils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static me.fengorz.kiwi.common.tts.TtsConstants.BEAN_NAMES.BAIDU_TTS_SERVICE_IMPL;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Slf4j
@ActiveProfiles({EnvConstants.TEST})
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = WordBizApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ReviewServiceTest {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private WordMainService wordMainService;

    @Autowired
    private OperateService operateService;

    @Autowired
    private ParaphraseService paraphraseService;

    @Resource(name = "fastDfsService")
    private DfsService dfsService;

    @Autowired
    private ParaphraseStarListService paraphraseStarListService;

    @Autowired
    private SeqService seqService;

    @Autowired
    private UserApi userApi;

    @Autowired
    private RevisePermanentAudioHelper revisePermanentAudioHelper;

    @Qualifier(BAIDU_TTS_SERVICE_IMPL)
    @Autowired
    private BaiduTtsService baiduTtsService;

    @Test
    @Disabled
    void initPermanent() {
        assertDoesNotThrow(() -> reviewService.initPermanent(false));
    }

    @Test
    @Disabled
    void generateTtsVoiceFromParaphraseId() {
        assertDoesNotThrow(() -> reviewService.generateTtsVoiceFromParaphraseId(2524421));
    }

    @Test
    @Disabled
    void generateTtsVoice() {
        assertDoesNotThrow(() -> reviewService.generateTtsVoice());
    }

    @Test
    @Disabled
    void createTheDays() {
        assertDoesNotThrow(() -> reviewService.createTheDays(ApiContants.ADMIN_ID));
    }

    @Test
    @Disabled
    void listReviewCounterVO() {
        List<WordReviewDailyCounterVO> list = reviewService.listReviewCounterVO(1);
        log.info("listReviewCounterVO >>>>>>>>>>>> {}", KiwiJsonUtils.toJsonStr(list));
        Assertions.assertEquals(ReviseDailyCounterTypeEnum.values().length, list.size());
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
        // List<ParaphraseDO> test = paraphraseService.listByWordName("test");
        // Assertions.assertNotNull(test);
        // ParaphraseDO paraphraseDO = test.get(0);
        // WordReviewAudioDO wordReviewAudio = reviewService.findWordReviewAudio(paraphraseDO.getParaphraseId(),
        // ReviseAudioTypeEnum.PARAPHRASE_EN.getType());

        // id=3749976, sourceId=2774291, type=0
        int sourceId = 1293771;
        Integer type = ReviseAudioTypeEnum.PARAPHRASE_EN.getType();
        WordReviewAudioDO wordReviewAudio =
                reviewService.findWordReviewAudio(sourceId, type);
        Assertions.assertNotNull(wordReviewAudio);
        log.info("wordReviewAudio.getSourceText() = {}", wordReviewAudio.getSourceText());
        log.info("wordReviewAudio.getSourceUrl() = {}", wordReviewAudio.getSourceUrl());
        // Assertions.assertEquals(3749976, wordReviewAudio.getId());
        byte[] bytes = this.dfsService.downloadFile(wordReviewAudio.getGroupName(), wordReviewAudio.getFilePath());
        FileUtil.writeBytes(bytes, String.format("/Users/zhanshifeng/Documents/temp/test_paraphrase_%d_%d.mp3", sourceId, type));
    }

    @SneakyThrows
    @Test
    @Disabled
    void test_findWordReviewAudio() {
        WordReviewAudioDO wordReviewAudio =
                reviewService.findWordReviewAudio(2774367, ReviseAudioTypeEnum.COMBO.getType());
        log.info("wordReviewAudio.getSourceText() = {}", wordReviewAudio.getSourceText());
        log.info("wordReviewAudio.getSourceUrl() = {}", wordReviewAudio.getSourceUrl());
        byte[] bytes = this.dfsService.downloadFile(wordReviewAudio.getGroupName(), wordReviewAudio.getFilePath());
        FileUtil.writeBytes(bytes, "/Users/zhanshifeng/Documents/temp/test_all.mp3");
    }

    @SneakyThrows
    @Test
    @Disabled
    void test_findCharacterReviewAudio() {
        WordReviewAudioDO wordReviewAudio = reviewService.findCharacterReviewAudio("adjective");
        log.info("wordReviewAudio.getSourceText() = {}", wordReviewAudio.getSourceText());
        log.info("wordReviewAudio.getSourceUrl() = {}", wordReviewAudio.getSourceUrl());
        byte[] bytes = this.dfsService.downloadFile(wordReviewAudio.getGroupName(), wordReviewAudio.getFilePath());
        FileUtil.writeBytes(bytes, "/Users/zhanshifeng/Documents/temp/test_adjective.mp3");
    }

    @Test
    @Disabled
    void test_createCommonParaphraseCollection() {
        R<?> echo = userApi.info("echo");
        Assertions.assertFalse(echo.getData() instanceof String);
        log.info("echo.getData(): {}", echo.getData());
        UserFullInfoDTO echoUserInfo = (UserFullInfoDTO) echo.getData();
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
        reviewService.evictWordReviewAudio(2774291, ReviseAudioTypeEnum.WORD_SPELLING.getType());
    }

    @Test
    @Disabled
    void test_revisePermanentAudioHelper() {
        int size1 = revisePermanentAudioHelper.getPermanentAudioEnums().size();
        int size2 = revisePermanentAudioHelper.getPermanentAudioEnumMap().size();
        int size3 = revisePermanentAudioHelper.getCacheStoreWithEnumKey().size();
        int size4 = revisePermanentAudioHelper.getCacheStoreWithEnumKey().size();
        Assertions.assertEquals(size1, size2);
        Assertions.assertEquals(size3, size2);
        Assertions.assertEquals(size3, size4);
        revisePermanentAudioHelper.getPermanentAudioEnums().forEach(audioEnum -> Assertions
                .assertNotNull(revisePermanentAudioHelper.getCacheStoreWithEnumKey().get(audioEnum)));
    }

    @Test
    @Disabled
    void test_removeWordReviewAudio() {
        reviewService.removeWordReviewAudio(2447981);
    }

    @Test
    @Disabled
    @SneakyThrows
    void test_baidu_tts() {
        byte[] bytes = baiduTtsService.speech("A。B。C");
        FileUtils.writeByteArrayToFile(new File("baiduTts.mp3"), bytes);
    }

    @SneakyThrows
    @Disabled
    @Test
    void test_generateParaphraseAllAudio() {
        assertDoesNotThrow(() -> {
            WordQueryVO test = operateService.queryWord("test");
            ParaphraseVO paraphraseVO = test.getCharacterVOList().get(0).getParaphraseVOList().get(0);
            reviewService.removeWordReviewAudio(paraphraseVO.getParaphraseId());
            reviewService.generateTtsVoiceFromParaphraseId(paraphraseVO.getParaphraseId());
        });
    }

    @SneakyThrows
    // @Disabled
    @Test
    void test_reGenReviewAudioForParaphrase() {
        assertDoesNotThrow(() -> {
            reviewService.reGenReviewAudioForParaphrase(5570452);
        });
    }

}