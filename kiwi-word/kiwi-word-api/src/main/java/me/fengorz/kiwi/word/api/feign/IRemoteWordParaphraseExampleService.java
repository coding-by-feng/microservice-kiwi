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
import me.fengorz.kiwi.word.api.entity.WordParaphraseExampleDO;
import me.fengorz.kiwi.word.api.feign.factory.RemoteWordParaphraseExampleServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


/**
 * 单词例句表
 *
 * @author codingByFeng
 * @date 2019-11-01 14:43:28
 */
@FeignClient(contextId = "remoteWordParaphraseExampleService", value = CrawlerConstants.VOCABULARY_ENHANCER_CRAWLER_BIZ, fallbackFactory = RemoteWordParaphraseExampleServiceFallbackFactory.class)
public interface IRemoteWordParaphraseExampleService {

    String WORD_PARAPHRASE_EXAMPLE = "/word/paraphrase/example";

    /*
     * 分页查询
     */
    @GetMapping(WORD_PARAPHRASE_EXAMPLE + "/page")
    R getWordParaphraseExamplePage(Page page, WordParaphraseExampleDO wordParaphraseExampleDO);

    /*
     * 根据条件查询单个实体
     */
    @PostMapping(WORD_PARAPHRASE_EXAMPLE + "/getOne")
    R getOne(@RequestBody WordParaphraseExampleDO condition);

    /*
     * 通过id查询单词例句表
     */
    @GetMapping(WORD_PARAPHRASE_EXAMPLE + "/{exampleId}")
    R getById(@PathVariable("exampleId") Integer exampleId);

    /*
     * 新增
     */
    @PostMapping(WORD_PARAPHRASE_EXAMPLE + "/save")
    R save(@RequestBody WordParaphraseExampleDO wordParaphraseExampleDO);

    /*
     * 修改
     */
    @PutMapping(WORD_PARAPHRASE_EXAMPLE + "/updateById")
    R updateById(@RequestBody WordParaphraseExampleDO wordParaphraseExampleDO);

    /*
     * 通过id删除
     */
    @DeleteMapping(WORD_PARAPHRASE_EXAMPLE + "/{exampleId}")
    R removeById(@PathVariable Integer exampleId);
}
