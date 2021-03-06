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
import me.fengorz.kiwi.common.api.annotation.log.SysLog;
import me.fengorz.kiwi.common.api.exception.dfs.DfsOperateException;
import me.fengorz.kiwi.common.fastdfs.service.IDfsService;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.common.sdk.web.WebTools;
import me.fengorz.kiwi.word.api.entity.PronunciationDO;
import me.fengorz.kiwi.word.biz.service.base.IPronunciationService;
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
public class PronunciationController extends BaseController {

    private final IPronunciationService wordPronunciationService;
    private final IDfsService dfsService;

    @SysLog("下载播放单词发音音频")
    @GetMapping("/downloadVoice/{pronunciationId}")
    public void downloadVoice(HttpServletResponse response, @PathVariable("pronunciationId") Integer pronunciationId) {
        PronunciationDO wordPronunciation = this.wordPronunciationService.getById(pronunciationId);
        if (wordPronunciation == null) {
            return;
        }
        InputStream inputStream = null;
        try {
            inputStream =
                this.dfsService.downloadStream(wordPronunciation.getGroupName(), wordPronunciation.getVoiceFilePath());
        } catch (DfsOperateException e) {
            log.error("downloadVoice exception, pronunciationId={}!", pronunciationId, e);
        }
        WebTools.downloadResponse(response, inputStream);
    }

}
