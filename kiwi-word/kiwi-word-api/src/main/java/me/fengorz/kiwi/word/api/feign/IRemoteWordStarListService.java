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
import me.fengorz.kiwi.word.api.entity.WordStarListDO;
import me.fengorz.kiwi.word.api.feign.factory.RemoteWordStarListServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


/**
 * 单词本
 *
 * @author codingByFeng
 * @date 2019-12-08 23:26:57
 */
@FeignClient(contextId = "remoteWordStarListDOService", value = CrawlerConstants.VOCABULARY_ENHANCER_CRAWLER_BIZ, fallbackFactory = RemoteWordStarListServiceFallbackFactory.class)
public interface IRemoteWordStarListService {

    String WORD_STAR_LIST = "/word/star/list";

    /*
     * 分页查询
     */
    @GetMapping(WORD_STAR_LIST + "/page")
    R getWordStarListPage(Page page, WordStarListDO wordStarListDO);

    /*
     * 根据条件查询单个实体
     */
    @PostMapping(WORD_STAR_LIST + "/getOne")
    R getOne(@RequestBody WordStarListDO condition);

    /*
     * 通过id查询单词本
     */
    @GetMapping(WORD_STAR_LIST + "/{id}")
    R getById(@PathVariable("id") Integer id);

    /*
     * 新增
     */
    @PostMapping(WORD_STAR_LIST + "/save")
    R save(@RequestBody WordStarListDO wordStarListDO);

    /*
     * 修改
     */
    @PutMapping(WORD_STAR_LIST + "/updateById")
    R updateById(@RequestBody WordStarListDO wordStarListDO);

    /*
     * 通过id删除
     */
    @DeleteMapping(WORD_STAR_LIST + "/{id}")
    R removeById(@PathVariable Integer id);
}
