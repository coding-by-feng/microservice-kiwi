/*
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
 */
package me.fengorz.kiwi.word.biz.controller;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.fastdfs.service.DfsService;
import me.fengorz.kiwi.common.sdk.controller.AbstractDfsController;
import me.fengorz.kiwi.common.sdk.exception.DataCheckedException;
import me.fengorz.kiwi.common.sdk.exception.dfs.DfsOperateException;
import me.fengorz.kiwi.common.sdk.exception.tts.TtsException;
import me.fengorz.kiwi.common.sdk.web.WebTools;
import me.fengorz.kiwi.common.sdk.web.security.SecurityUtils;
import me.fengorz.kiwi.common.tts.service.TtsService;
import me.fengorz.kiwi.word.api.entity.WordReviewAudioDO;
import me.fengorz.kiwi.word.api.vo.WordReviewDailyCounterVO;
import me.fengorz.kiwi.word.biz.service.operate.OperateService;
import me.fengorz.kiwi.word.biz.service.operate.ReviewService;

/**
 * @author zhanShiFeng
 * @date 2021-08-19 20:42:11
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/word/review/")
public class WordReviewController extends AbstractDfsController {

    private final ReviewService reviewService;
    private final OperateService operateService;
    private final DfsService dfsService;
    private final TtsService ttsService;

    @GetMapping("/getReviewBreakpointPageNumber/{listId}")
    public R<Integer> getReviewBreakpointPageNumber(@PathVariable Integer listId) {
        return R.success(reviewService.getReviewBreakpointPageNumber(listId));
    }

    @GetMapping("/createTheDays")
    public R<Void> createTheDays() {
        reviewService.createTheDays(SecurityUtils.getCurrentUserId());
        return R.success();
    }

    @GetMapping("/refreshAllApiKey")
    public R<Void> refreshAllApiKey() {
        ttsService.refreshAllApiKey();
        return R.success();
    }

    @GetMapping("/getReviewCounterVO/{type}")
    public R<WordReviewDailyCounterVO> getReviewCounterVO(@PathVariable("type") Integer type) {
        return R.success(reviewService.findReviewCounterVO(SecurityUtils.getCurrentUserId(), type));
    }

    @GetMapping("/getAllReviewCounterVO")
    public R<List<WordReviewDailyCounterVO>> getAllReviewCounterVO() {
        return R.success(reviewService.listReviewCounterVO(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/downloadReviewAudio/{sourceId}/{type}")
    public void downloadReviewAudio(HttpServletResponse response, @PathVariable("sourceId") Integer sourceId,
        @PathVariable("type") Integer type) {
        WordReviewAudioDO wordReviewAudio = null;
        try {
            wordReviewAudio = this.reviewService.findWordReviewAudio(sourceId, type);
        } catch (DfsOperateException | TtsException | DataCheckedException e) {
            log.error("findWordReviewAudio exception, sourceId={}, type={}!", sourceId, type, e);
        }
        if (wordReviewAudio == null) {
            log.error("=========> Required wordReviewAudio must not be null!");
            return;
        } else {
            log.info("Required wordReviewAudio is found.");
        }
        InputStream inputStream = null;
        try {
            byte[] bytes = this.dfsService.downloadFile(wordReviewAudio.getGroupName(), wordReviewAudio.getFilePath());
            log.info("Required wordReviewAudio bytes download success.");
            inputStream = new ByteArrayInputStream(bytes);
            response.addHeader(CONTENT_TYPE, AUDIO_MPEG);
            response.addHeader(ACCEPT_RANGES, BYTES);
            response.addHeader(CONTENT_LENGTH, String.valueOf(bytes.length));
        } catch (DfsOperateException e) {
            log.error("downloadReviewAudio exception, sourceId={}, type={}!", sourceId, type, e);
        }
        WebTools.downloadResponse(response, inputStream);
        log.info("Method downloadResponse invoked success.");
    }

    @GetMapping("/generateTtsVoice/{isReplace}")
    public R<Void> generateTtsVoice(@PathVariable("isReplace") Boolean isReplace) {
        try {
            reviewService.generateTtsVoice(isReplace);
            return R.success();
        } catch (Exception e) {
            log.error("generateTtsVoice error!", e);
        }
        return R.error();
    }

    @GetMapping("/generateTtsVoiceFromParaphraseId/{paraphraseId}")
    public R<Void> generateTtsVoiceFromParaphraseId(@PathVariable("paraphraseId") Integer paraphraseId) {
        try {
            reviewService.generateTtsVoiceFromParaphraseId(paraphraseId);
        } catch (DfsOperateException | TtsException | DataCheckedException e) {
            log.error("generateTtsVoiceFromParaphraseId exception, paraphraseId={}!", paraphraseId, e);
            return R.failed();
        }
        return R.success();
    }

    @GetMapping("/increaseCounter/{type}")
    public R<Void> increaseCounter(@PathVariable("type") Integer type) {
        reviewService.increase(type, SecurityUtils.getCurrentUserId());
        return R.success();
    }

    @GetMapping("/autoSelectApiKey")
    public R<String> autoSelectApiKey() {
        return R.success(ttsService.autoSelectApiKey());
    }

    @GetMapping("/increaseApiKeyUsedTime/{apiKey}")
    public R<Void> increaseApiKeyUsedTime(@PathVariable("apiKey") String apiKey) {
        ttsService.increaseApiKeyUsedTime(apiKey);
        return R.success();
    }

    @GetMapping("/deprecateApiKeyToday/{apiKey}")
    public R<Void> deprecateApiKeyToday(@PathVariable("apiKey") String apiKey) {
        ttsService.deprecateApiKeyToday(apiKey);
        return R.success();
    }

}
