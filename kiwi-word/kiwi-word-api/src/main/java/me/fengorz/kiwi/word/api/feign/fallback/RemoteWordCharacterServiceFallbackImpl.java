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

package me.fengorz.kiwi.word.api.feign.fallback;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.word.api.entity.WordCharacterDO;
import me.fengorz.kiwi.word.api.feign.IRemoteWordCharacterService;
import org.springframework.stereotype.Component;


/**
 * 单词词性表
 *
 * @author codingByFeng
 * @date 2019-11-01 14:37:07
 */
@Slf4j
@Component
public class RemoteWordCharacterServiceFallbackImpl implements IRemoteWordCharacterService {

    @Setter
    private Throwable throwable;

    @Override
    public R getWordCharacterPage(Page page, WordCharacterDO wordCharacter) {
        log.error("getWordCharacterPage error, wordCharacter=" + wordCharacter, throwable);
        return null;
    }

    @Override
    public R getOne(WordCharacterDO condition) {
        log.error("getOne error, wordCharacter=" + condition, throwable);
        return null;
    }

    @Override
    public R getById(Integer characterId) {
        log.error("getById error, characterId=" + characterId, throwable);
        return null;
    }

    @Override
    public R save(WordCharacterDO WordCharacter) {
        log.error("save error, WordCharacterDO=" + WordCharacter, throwable);
        return null;
    }

    @Override
    public R updateById(WordCharacterDO wordCharacter) {
        log.error("updateById error, WordCharacterDO=" + wordCharacter, throwable);
        return null;
    }

    @Override
    public R removeById(Integer characterId) {
        log.error("removeById error, characterId=" + characterId, throwable);
        return null;
    }
}

