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
import me.fengorz.kiwi.word.api.common.WordConstants;
import me.fengorz.kiwi.word.api.entity.WordParaphraseStarListDO;
import me.fengorz.kiwi.word.api.feign.factory.RemoteWordParaphraseStarListServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


/**
 * 单词本
 *
 * @author codingByFeng
 * @date 2019-12-08 23:27:41
 */
@FeignClient(contextId = "remoteWordParaphraseStarListDOService", value = WordConstants.KIWI_WORD_BIZ, fallbackFactory = RemoteWordParaphraseStarListServiceFallbackFactory.class)
public interface IRemoteWordParaphraseStarListService {

    String WORD_PARAPHRASE_STAR = "/word/paraphrase/star";

    /*
     * 分页查询
     */
    @GetMapping(WORD_PARAPHRASE_STAR + "/list/page")
    R getWordParaphraseStarListPage(Page page, WordParaphraseStarListDO wordParaphraseStarListDO);

    /*
     * 根据条件查询单个实体
     */
    @PostMapping(WORD_PARAPHRASE_STAR + "/list/getOne")
    R getOne(@RequestBody WordParaphraseStarListDO condition);

    /*
     * 通过id查询单词本
     */
    @GetMapping(WORD_PARAPHRASE_STAR + "/list/{id}")
    R getById(@PathVariable("id") Integer id);

    /*
     * 新增
     */
    @PostMapping(WORD_PARAPHRASE_STAR + "/list/save")
    R save(@RequestBody WordParaphraseStarListDO wordParaphraseStarListDO);

    /*
     * 修改
     */
    @PutMapping(WORD_PARAPHRASE_STAR + "/list/updateById")
    R updateById(@RequestBody WordParaphraseStarListDO wordParaphraseStarListDO);

    /*
     * 通过id删除
     */
    @DeleteMapping(WORD_PARAPHRASE_STAR + "/list/{id}")
    R removeById(@PathVariable Integer id);
}
