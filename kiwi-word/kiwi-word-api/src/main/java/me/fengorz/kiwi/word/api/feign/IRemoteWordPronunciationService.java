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
package me.fengorz.kiwi.word.api.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.word.api.common.CrawlerConstants;
import me.fengorz.kiwi.word.api.entity.WordPronunciationDO;
import me.fengorz.kiwi.word.api.feign.factory.RemoteWordPronunciationServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


/**
 * 单词例句表
 *
 * @author codingByFeng
 * @date 2019-11-01 14:44:45
 */
@FeignClient(contextId = "remoteWordPronunciationService", value = CrawlerConstants.VOCABULARY_ENHANCER_CRAWLER_BIZ, fallbackFactory = RemoteWordPronunciationServiceFallbackFactory.class)
public interface IRemoteWordPronunciationService {

    String WORD_PRONUNCIATION = "/word/pronunciation";

    /*
     * 分页查询
     */
    @GetMapping(WORD_PRONUNCIATION + "/page")
    R getWordPronunciationPage(Page page, WordPronunciationDO wordPronunciation);

    /*
     * 根据条件查询单个实体
     */
    @PostMapping(WORD_PRONUNCIATION + "/getOne")
    R getOne(@RequestBody WordPronunciationDO condition);

    /*
     * 通过id查询单词例句表
     */
    @GetMapping(WORD_PRONUNCIATION + "/{pronunciationId}")
    R getById(@PathVariable("pronunciationId") Integer pronunciationId);

    /*
     * 新增
     */
    @PostMapping(WORD_PRONUNCIATION + "/save")
    R save(@RequestBody WordPronunciationDO wordPronunciation);

    /*
     * 修改
     */
    @PutMapping(WORD_PRONUNCIATION + "/updateById")
    R updateById(@RequestBody WordPronunciationDO wordPronunciation);

    /*
     * 通过id删除
     */
    @DeleteMapping(WORD_PRONUNCIATION + "/{pronunciationId}")
    R removeById(@PathVariable Integer pronunciationId);
}
