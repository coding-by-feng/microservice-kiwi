/*
 *
 * Copyright [2019~2025] [zhanshifeng]
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
package me.fengorz.kiwi.word.biz.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.dfs.DfsService;
import me.fengorz.kiwi.common.sdk.annotation.log.LogMarker;
import me.fengorz.kiwi.common.sdk.controller.AbstractFileController;
import me.fengorz.kiwi.common.sdk.exception.dfs.DfsOperateException;
import me.fengorz.kiwi.common.sdk.web.WebTools;
import me.fengorz.kiwi.word.api.entity.PronunciationDO;
import me.fengorz.kiwi.word.biz.service.base.PronunciationService;
import me.fengorz.kiwi.word.biz.service.operate.CrawlerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

/**
 * 单词例句表
 *
 * @author zhanshifeng
 * @date 2019-10-31 20:54:06
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/word/pronunciation")
@Slf4j
public class PronunciationController extends AbstractFileController {

    private final PronunciationService wordPronunciationService;
    private DfsService dfsService;
    private final CrawlerService crawlerService;

    @LogMarker("下载播放单词发音音频")
    @GetMapping("/downloadVoice/{pronunciationId}")
    public void downloadVoice(HttpServletResponse response, @PathVariable("pronunciationId") Integer pronunciationId) {
        PronunciationDO wordPronunciation = this.wordPronunciationService.getById(pronunciationId);
        if (wordPronunciation == null) {
            log.error("=========> Required wordPronunciation must not be null!");
            return;
        } else {
            log.info("Required wordPronunciation is found.");
        }
        InputStream inputStream = null;
        try {
            byte[] bytes =
                    this.dfsService.downloadFile(wordPronunciation.getGroupName(), wordPronunciation.getVoiceFilePath());
            log.info("Required wordPronunciation bytes download success.");
            inputStream = buildInputStream(response, bytes);
        } catch (DfsOperateException e) {
            log.error("downloadVoice exception, pronunciationId={}, re-fetching now!", pronunciationId, e);
            crawlerService.reFetchPronunciation(pronunciationId);
        }
        WebTools.downloadResponseAndClose(response, inputStream, true);
        log.info("Method downloadResponse for wordPronunciation invoked success.");
    }
}
