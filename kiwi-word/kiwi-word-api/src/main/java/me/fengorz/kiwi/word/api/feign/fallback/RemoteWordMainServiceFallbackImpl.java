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

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.word.api.dto.fetch.FetchWordResultDTO;
import me.fengorz.kiwi.word.api.dto.remote.WordMainPageDTO;
import me.fengorz.kiwi.word.api.entity.WordMainDO;
import me.fengorz.kiwi.word.api.feign.IRemoteWordMainService;
import org.springframework.stereotype.Component;


/**
 * 单词主表
 *
 * @author codingByFeng
 * @date 2019-11-01 14:29:33
 */
@Slf4j
@Component
public class RemoteWordMainServiceFallbackImpl implements IRemoteWordMainService {

    @Setter
    private Throwable throwable;

    @Override
    public R test(FetchWordResultDTO fetchWordResultDTO) {
        log.error("test error, fetchWordResultDTO=" + fetchWordResultDTO, throwable);
        return null;
    }

    @Override
    public R getWordMainPage(WordMainPageDTO wordMainPage) {
        log.error("getWordMainPage error, wordMainPage=" + wordMainPage, throwable);
        return null;
    }

    @Override
    public R getOne(WordMainDO condition) {
        log.error("getOne error, wordMainDO=" + condition, throwable);
        return null;
    }

    @Override
    public R getById(Integer wordId) {
        log.error("getById error, wordId=" + wordId, throwable);
        return null;
    }

    @Override
    public R save(WordMainDO WordMainDO) {
        log.error("save error, WordMainDO=" + WordMainDO, throwable);
        return null;
    }

    @Override
    public R updateById(WordMainDO wordMainDO) {
        log.error("updateById error, WordMainDO=" + wordMainDO, throwable);
        return null;
    }

    @Override
    public R removeById(Integer wordId) {
        log.error("removeById error, wordId=" + wordId, throwable);
        return null;
    }
}

