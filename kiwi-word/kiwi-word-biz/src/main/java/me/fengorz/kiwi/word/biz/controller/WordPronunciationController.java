/*
 *
 *   Copyright [2019~2025] [codingByFeng]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *
 */
package me.fengorz.kiwi.word.biz.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.api.annotation.log.SysLog;
import me.fengorz.kiwi.common.api.exception.dfs.DfsOperateException;
import me.fengorz.kiwi.common.fastdfs.service.IDfsService;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.common.sdk.web.WebTools;
import me.fengorz.kiwi.word.api.entity.WordPronunciationDO;
import me.fengorz.kiwi.word.biz.service.IWordPronunciationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;


/**
 * 单词例句表
 *
 * @author codingByFeng
 * @date 2019-10-31 20:54:06
 */
@RestController
@AllArgsConstructor
@RequestMapping("/word/pronunciation")
@Slf4j
public class WordPronunciationController extends BaseController {

    private final IWordPronunciationService wordPronunciationService;
    private final IDfsService dfsService;

    /**
     * 分页查询
     *
     * @param page              分页对象
     * @param wordPronunciation 单词例句表
     * @return
     */
    @GetMapping("/page")
    public R getWordPronunciationPage(Page page, WordPronunciationDO wordPronunciation) {
        return R.success(wordPronunciationService.page(page, Wrappers.query(wordPronunciation)));
    }


    /**
     * 通过id查询单词例句表
     *
     * @param pronunciationId id
     * @return R
     */
    @GetMapping("/{pronunciationId}")
    public R getById(@PathVariable("pronunciationId") Integer pronunciationId) {
        return R.success(wordPronunciationService.getById(pronunciationId));
    }

    /**
     * 新增单词例句表
     *
     * @param wordPronunciation 单词例句表
     * @return R
     */
    @SysLog("新增单词例句表")
    @PostMapping
    @PreAuthorize("@pms.hasPermission('biz_wordpronunciation_add')")
    public R save(@RequestBody WordPronunciationDO wordPronunciation) {
        return R.success(wordPronunciationService.save(wordPronunciation));
    }

    /**
     * 修改单词例句表
     *
     * @param wordPronunciation 单词例句表
     * @return R
     */
    @SysLog("修改单词例句表")
    @PutMapping
    @PreAuthorize("@pms.hasPermission('biz_wordpronunciation_edit')")
    public R updateById(@RequestBody WordPronunciationDO wordPronunciation) {
        return R.success(wordPronunciationService.updateById(wordPronunciation));
    }

    /**
     * 通过id删除单词例句表
     *
     * @param pronunciationId id
     * @return R
     */
    @SysLog("通过id删除单词例句表")
    @DeleteMapping("/{pronunciationId}")
    @PreAuthorize("@pms.hasPermission('biz_wordpronunciation_del')")
    public R removeById(@PathVariable Integer pronunciationId) {
        return R.success(wordPronunciationService.removeById(pronunciationId));
    }

    @SysLog("下载播放单词发音音频")
    @GetMapping("/downloadVoice/{pronunciationId}")
    public void downloadVoice(HttpServletResponse response, @PathVariable("pronunciationId") Integer pronunciationId) {
        WordPronunciationDO wordPronunciation = this.wordPronunciationService.getById(pronunciationId);
        if (wordPronunciation == null) {
            return;
        }
        InputStream inputStream = null;
        try {
            inputStream = this.dfsService.downloadStream(wordPronunciation.getGroupName(), wordPronunciation.getVoiceFilePath());
        } catch (DfsOperateException e) {
            log.error("downloadVoice exception, pronunciationId={}!" , pronunciationId, e);
        }
        WebTools.downloadResponse(response, inputStream);
    }

}
