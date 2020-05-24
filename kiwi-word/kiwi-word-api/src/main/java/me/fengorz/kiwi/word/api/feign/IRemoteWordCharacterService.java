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
import me.fengorz.kiwi.word.api.entity.WordCharacterDO;
import me.fengorz.kiwi.word.api.feign.factory.RemoteWordCharacterServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


/**
 * 单词词性表
 *
 * @author codingByFeng
 * @date 2019-11-01 14:37:07
 */
@FeignClient(contextId = "remoteWordCharacterService", value = WordConstants.KIWI_WORD_BIZ, fallbackFactory = RemoteWordCharacterServiceFallbackFactory.class)
public interface IRemoteWordCharacterService {

    String WORD_CHARACTER = "/word/character";

    /*
     * 分页查询
     */
    @GetMapping(WORD_CHARACTER + "/page")
    R getWordCharacterPage(Page page, WordCharacterDO wordCharacter);

    /*
     * 根据条件查询单个实体
     */
    @PostMapping(WORD_CHARACTER + "/getOne")
    R getOne(@RequestBody WordCharacterDO condition);

    /*
     * 通过id查询单词词性表
     */
    @GetMapping(WORD_CHARACTER + "/{characterId}")
    R getById(@PathVariable("characterId") Integer characterId);

    /*
     * 新增
     */
    @PostMapping(WORD_CHARACTER + "/save")
    R save(@RequestBody WordCharacterDO wordCharacter);

    /*
     * 修改
     */
    @PutMapping(WORD_CHARACTER + "/updateById")
    R updateById(@RequestBody WordCharacterDO wordCharacter);

    /*
     * 通过id删除
     */
    @DeleteMapping(WORD_CHARACTER + "/{characterId}")
    R removeById(@PathVariable Integer characterId);
}
