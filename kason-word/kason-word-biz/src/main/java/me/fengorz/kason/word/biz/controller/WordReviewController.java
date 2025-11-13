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
package me.fengorz.kason.word.biz.controller;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kason.common.api.R;
import me.fengorz.kason.common.dfs.DfsService;
import me.fengorz.kason.common.sdk.controller.AbstractFileController;
import me.fengorz.kason.common.sdk.exception.DataCheckedException;
import me.fengorz.kason.common.sdk.exception.ResourceNotFoundException;
import me.fengorz.kason.common.sdk.exception.ServiceException;
import me.fengorz.kason.common.sdk.exception.dfs.DfsOperateException;
import me.fengorz.kason.common.sdk.exception.tts.TtsException;
import me.fengorz.kason.common.sdk.web.WebTools;
import me.fengorz.kason.common.sdk.web.security.SecurityUtils;
import me.fengorz.kason.common.tts.service.TtsService;
import me.fengorz.kason.word.api.common.enumeration.ReviseAudioTypeEnum;
import me.fengorz.kason.word.api.entity.WordReviewAudioDO;
import me.fengorz.kason.word.api.vo.WordReviewDailyCounterVO;
import me.fengorz.kason.word.biz.service.operate.ReviewService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author zhanShiFeng
 * @date 2021-08-19 20:42:11
 */
@Slf4j
@RestController
@RequestMapping("/word/review/")
public class WordReviewController extends AbstractFileController {

    private final ReviewService reviewService;
    private final TtsService deepgramTtsService;
    @Resource(name = "fastDfsService")
    private DfsService dfsService;

    public WordReviewController(ReviewService reviewService,
                                @Qualifier("deepgramAura2TtsService") TtsService deepgramTtsService) {
        this.reviewService = reviewService;
        this.deepgramTtsService = deepgramTtsService;
    }

    @GetMapping("/getReviewBreakpointPageNumber/{listId}")
    public R<Integer> getReviewBreakpointPageNumber(@PathVariable Integer listId) {
        return R.success(reviewService.getReviewBreakpointPageNumber(listId));
    }

    @PostMapping("/createTheDays")
    public R<Void> createTheDays() {
        reviewService.createTheDays(SecurityUtils.getCurrentUserId());
        return R.success();
    }

    @GetMapping("/refreshAllApiKey")
    public R<Void> refreshAllApiKey() {
        deepgramTtsService.refreshAllApiKey();
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

    @SuppressWarnings("ConstantConditions")
    @GetMapping("/downloadReviewAudio/{sourceId}/{type}")
    public void downloadReviewAudio(HttpServletResponse response, @PathVariable("sourceId") Integer sourceId,
                                    @PathVariable("type") Integer type) {
        log.info("downloadReviewAudio, sourceId={}, type={}", sourceId, type);
        WordReviewAudioDO wordReviewAudio = null;
        try {
            wordReviewAudio = this.reviewService.findWordReviewAudio(sourceId, type);
        } catch (DfsOperateException | TtsException | DataCheckedException e) {
            log.error("findWordReviewAudio exception, sourceId={}, type={}!", sourceId, type, e);
        }
        if (wordReviewAudio == null) {
            log.error("=========> Required word review audio must not be null!");
            throw new ServiceException();
        }
        InputStream inputStream = null;
        try {
            inputStream = prepareInputStream(response, sourceId, type, wordReviewAudio);
        } catch (DfsOperateException e) {
            log.error("downloadReviewAudio exception, sourceId={}, type={}, {}", sourceId, ReviseAudioTypeEnum.fromValue(type).name(), e.getMessage());
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException ex) {
                log.error("Input stream close failed, {}", ex.getMessage());
            }
            try {
                wordReviewAudio = this.reviewService.generateWordReviewAudio(sourceId, type);
                inputStream = prepareInputStream(response, sourceId, type, wordReviewAudio);
            } catch (DfsOperateException | TtsException | DataCheckedException ex) {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (IOException exc) {
                    log.error("Input stream close failed.", exc);
                }
                throw new ResourceNotFoundException("Revise audio resource acquire failed.");
            }
        }
        WebTools.downloadResponseAndClose(response, inputStream);
        log.info("Method downloadResponse for wordReviewAudio invoked success, sourceId={}, type={}", sourceId, type);
    }

    private InputStream prepareInputStream(HttpServletResponse response, Integer sourceId, Integer type, WordReviewAudioDO wordReviewAudio) throws DfsOperateException {
        byte[] bytes = this.dfsService.downloadFile(wordReviewAudio.getGroupName(), wordReviewAudio.getFilePath());
        log.info("Required wordReviewAudio bytes download success, sourceId={}, type={}", sourceId, type);
        return buildInputStream(response, bytes);
    }

    @GetMapping("/character/downloadReviewAudio/{characterCode}")
    public void downloadCharacterReviewAudio(HttpServletResponse response,
                                             @PathVariable("characterCode") String characterCode) {
        log.info("downloadCharacterReviewAudio, characterCode={}", characterCode);
        WordReviewAudioDO wordReviewAudio = this.reviewService.findCharacterReviewAudio(characterCode);
        if (wordReviewAudio == null) {
            log.error("=========> Required character eeview audio must not be null!");
            throw new ServiceException();
        }
        InputStream inputStream = null;
        try {
            byte[] bytes = this.dfsService.downloadFile(wordReviewAudio.getGroupName(), wordReviewAudio.getFilePath());
            log.info("Required wordReviewAudio bytes download success, characterCode={}, bytes length={}", characterCode, bytes.length);
            inputStream = buildInputStream(response, bytes);
        } catch (DfsOperateException e) {
            log.error("downloadReviewAudio exception, characterCode={}!", characterCode, e);
        }
        WebTools.downloadResponseAndClose(response, inputStream);
    }

    @Deprecated
    @PostMapping("/generateTtsVoiceFromParaphraseId/{paraphraseId}")
    public R<Void> generateTtsVoiceFromParaphraseId(@PathVariable("paraphraseId") Integer paraphraseId) {
        try {
            reviewService.generateTtsVoiceFromParaphraseId(paraphraseId);
        } catch (DfsOperateException | TtsException | DataCheckedException e) {
            log.error("generateTtsVoiceFromParaphraseId exception, paraphraseId={}!", paraphraseId, e);
            return R.failed();
        }
        return R.success();
    }

    @PutMapping("/increaseCounter/{type}")
    public R<Void> increaseCounter(@PathVariable("type") Integer type) {
        reviewService.increase(type, SecurityUtils.getCurrentUserId());
        return R.success();
    }

    @GetMapping("/autoSelectApiKey")
    public R<String> autoSelectApiKey() {
        return R.success(deepgramTtsService.autoSelectApiKey());
    }

    @PutMapping("/increaseApiKeyUsedTime/{apiKey}")
    public R<Void> increaseApiKeyUsedTime(@PathVariable("apiKey") String apiKey) {
        deepgramTtsService.increaseApiKeyUsedTime(apiKey);
        return R.success();
    }

    @PutMapping("/deprecateApiKeyToday/{apiKey}")
    public R<Void> deprecateApiKeyToday(@PathVariable("apiKey") String apiKey) {
        deepgramTtsService.deprecateApiKeyToday(apiKey);
        return R.success();
    }

    @DeleteMapping("/deprecate-review-audio/{sourceId}")
    public R<Void> deprecateReviewAudio(@PathVariable("sourceId") Integer sourceId) {
        reviewService.removeWordReviewAudio(sourceId);
        return R.success();
    }

    @DeleteMapping("/reGenReviewAudio/{sourceId}")
    public R<Void> reGenReviewAudioForParaphrase(@PathVariable("sourceId") Integer sourceId) {
        try {
            reviewService.reGenReviewAudioForParaphrase(sourceId);
        } catch (Exception e) {
            log.error("reGenReviewAudio exception, sourceId={}!", sourceId, e);
            return R.failed();
        }
        return R.success();
    }

}
