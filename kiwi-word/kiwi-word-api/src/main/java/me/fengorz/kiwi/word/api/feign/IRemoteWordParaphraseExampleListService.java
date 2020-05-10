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
import me.fengorz.kiwi.word.api.entity.WordExampleStarListDO;
import me.fengorz.kiwi.word.api.feign.factory.RemoteWordParaphraseExampleListServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


/**
 * 
 *
 * @author codingByFeng
 * @date 2019-12-08 23:27:12
 */
@FeignClient(contextId = "remoteWordParaphraseExampleListDOService", value = CrawlerConstants.VOCABULARY_ENHANCER_CRAWLER_BIZ, fallbackFactory = RemoteWordParaphraseExampleListServiceFallbackFactory.class)
public interface IRemoteWordParaphraseExampleListService {

    String WORD_PARAPHRASE_EXAMPLE_LIST = "/word/paraphrase/example/list";

    /*
     * 分页查询
     */
    @GetMapping(WORD_PARAPHRASE_EXAMPLE_LIST + "/page")
    R getWordParaphraseExampleListPage(Page page, WordExampleStarListDO wordExampleStarListDO);

    /*
     * 根据条件查询单个实体
     */
    @PostMapping(WORD_PARAPHRASE_EXAMPLE_LIST + "/getOne")
    R getOne(@RequestBody WordExampleStarListDO condition);

    /*
     * 通过id查询
     */
    @GetMapping(WORD_PARAPHRASE_EXAMPLE_LIST + "/{id}")
    R getById(@PathVariable("id") Integer id);

    /*
     * 新增
     */
    @PostMapping(WORD_PARAPHRASE_EXAMPLE_LIST + "/save")
    R save(@RequestBody WordExampleStarListDO wordExampleStarListDO);

    /*
     * 修改
     */
    @PutMapping(WORD_PARAPHRASE_EXAMPLE_LIST + "/updateById")
    R updateById(@RequestBody WordExampleStarListDO wordExampleStarListDO);

    /*
     * 通过id删除
     */
    @DeleteMapping(WORD_PARAPHRASE_EXAMPLE_LIST + "/{id}")
    R removeById(@PathVariable Integer id);
}
