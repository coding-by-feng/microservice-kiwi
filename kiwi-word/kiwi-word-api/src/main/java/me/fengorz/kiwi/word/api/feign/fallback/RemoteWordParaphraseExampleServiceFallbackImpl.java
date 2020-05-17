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
import me.fengorz.kiwi.word.api.entity.WordParaphraseExampleDO;
import me.fengorz.kiwi.word.api.feign.IRemoteWordParaphraseExampleService;
import org.springframework.stereotype.Component;


/**
 * 单词例句表
 *
 * @author codingByFeng
 * @date 2019-11-01 14:43:28
 */
@Slf4j
@Component
public class RemoteWordParaphraseExampleServiceFallbackImpl implements IRemoteWordParaphraseExampleService {

    @Setter
    private Throwable throwable;

    @Override
    public R getWordParaphraseExamplePage(Page page, WordParaphraseExampleDO wordParaphraseExampleDO) {
        log.error("getWordParaphraseExamplePage error, wordParaphraseExampleDO=" + wordParaphraseExampleDO, throwable);
        return null;
    }

    @Override
    public R getOne(WordParaphraseExampleDO condition) {
        log.error("getOne error, wordParaphraseExample=" + condition, throwable);
        return null;
    }

    @Override
    public R getById(Integer exampleId) {
        log.error("getById error, exampleId=" + exampleId, throwable);
        return null;
    }

    @Override
    public R save(WordParaphraseExampleDO WordParaphraseExampleDO) {
        log.error("save error, WordParaphraseExampleDO=" + WordParaphraseExampleDO, throwable);
        return null;
    }

    @Override
    public R updateById(WordParaphraseExampleDO wordParaphraseExampleDO) {
        log.error("updateById error, WordParaphraseExampleDO=" + wordParaphraseExampleDO, throwable);
        return null;
    }

    @Override
    public R removeById(Integer exampleId) {
        log.error("removeById error, exampleId=" + exampleId, throwable);
        return null;
    }
}

