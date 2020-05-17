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

import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.word.api.common.WordCrawlerConstants;
import me.fengorz.kiwi.word.api.dto.fetch.FetchWordResultDTO;
import me.fengorz.kiwi.word.api.dto.remote.WordMainPageDTO;
import me.fengorz.kiwi.word.api.entity.WordMainDO;
import me.fengorz.kiwi.word.api.feign.factory.RemoteWordMainServiceFallBackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


/**
 * 单词主表
 *
 * @author codingByFeng
 * @date 2019-11-01 14:29:33
 */
@FeignClient(contextId = "remoteWordMainService", value = WordCrawlerConstants.VOCABULARY_ENHANCER_CRAWLER_BIZ, fallbackFactory = RemoteWordMainServiceFallBackFactory.class)
public interface IRemoteWordMainService {

    String WORD_MAIN = "/word/main";

    @PostMapping(WORD_MAIN + "/test")
    R test(FetchWordResultDTO fetchWordResultDTO);


    /*
     * 分页查询
     */
    @GetMapping(WORD_MAIN + "/page")
    R getWordMainPage(@RequestBody WordMainPageDTO wordMainPage);

    /*
     * 根据条件查询单个实体
     */
    @PostMapping(WORD_MAIN + "/getOne")
    R getOne(@RequestBody WordMainDO condition);

    /*
     * 通过id查询单词主表
     */
    @GetMapping(WORD_MAIN + "/{wordId}")
    R getById(@PathVariable("wordId") Integer wordId);

    /*
     * 新增
     */
    @PostMapping(WORD_MAIN + "/save")
    R save(@RequestBody WordMainDO wordMainDO);

    /*
     * 修改
     */
    @PutMapping(WORD_MAIN + "/updateById")
    R updateById(@RequestBody WordMainDO wordMainDO);

    /*
     * 通过id删除
     */
    @DeleteMapping(WORD_MAIN + "/{wordId}")
    R removeById(@PathVariable Integer wordId);
}
