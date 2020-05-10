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
import me.fengorz.kiwi.word.api.entity.WordParaphraseDO;
import me.fengorz.kiwi.word.api.feign.factory.RemoteWordParaphraseServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


/**
 * 单词释义表
 *
 * @author codingByFeng
 * @date 2019-11-01 14:41:24
 */
@FeignClient(contextId = "remoteWordParaphraseService", value = CrawlerConstants.VOCABULARY_ENHANCER_CRAWLER_BIZ, fallbackFactory = RemoteWordParaphraseServiceFallbackFactory.class)
public interface IRemoteWordParaphraseService {

    String WORD_PARAPHRASE = "/word/paraphrase";

    /*
     * 分页查询
     */
    @GetMapping(WORD_PARAPHRASE + "/page")
    R getWordParaphrasePage(Page page, WordParaphraseDO wordParaphraseDO);

    /*
     * 根据条件查询单个实体
     */
    @PostMapping(WORD_PARAPHRASE + "/getOne")
    R getOne(@RequestBody WordParaphraseDO condition);

    /*
     * 通过id查询单词释义表
     */
    @GetMapping(WORD_PARAPHRASE + "/{paraphraseId}")
    R getById(@PathVariable("paraphraseId") Integer paraphraseId);

    /*
     * 新增
     */
    @PostMapping(WORD_PARAPHRASE + "/save")
    R save(@RequestBody WordParaphraseDO wordParaphraseDO);

    /*
     * 修改
     */
    @PutMapping(WORD_PARAPHRASE + "/updateById")
    R updateById(@RequestBody WordParaphraseDO wordParaphraseDO);

    /*
     * 通过id删除
     */
    @DeleteMapping(WORD_PARAPHRASE + "/{paraphraseId}")
    R removeById(@PathVariable Integer paraphraseId);
}
