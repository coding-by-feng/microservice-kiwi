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
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.word.api.entity.WordParaphraseDO;
import me.fengorz.kiwi.word.api.feign.IRemoteWordParaphraseService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


/**
 * 单词释义表
 *
 * @author codingByFeng
 * @date 2019-11-01 14:41:24
 */
@Slf4j
@Component
public class RemoteWordParaphraseServiceFallbackImpl implements IRemoteWordParaphraseService {

    @Setter
    private Throwable throwable;

    @Override
    public R getWordParaphrasePage(Page page, WordParaphraseDO wordParaphraseDO) {
        log.error("getWordParaphrasePage error, wordParaphraseDO=" + wordParaphraseDO, throwable);
        return null;
    }

    @Override
    public R getOne(WordParaphraseDO condition) {
        log.error("getOne error, wordParaphrase=" + condition, throwable);
        return null;
    }

    @Override
    public R getById(Integer paraphraseId) {
        log.error("getById error, paraphraseId=" + paraphraseId, throwable);
        return null;
    }

    @Override
    public R save(WordParaphraseDO WordParaphraseDO) {
        log.error("save error, WordParaphraseDO=" + WordParaphraseDO, throwable);
        return null;
    }

    @Override
    public R updateById(WordParaphraseDO wordParaphraseDO) {
        log.error("updateById error, WordParaphraseDO=" + wordParaphraseDO, throwable);
        return null;
    }

    @Override
    public R removeById(Integer paraphraseId) {
        log.error("removeById error, paraphraseId=" + paraphraseId, throwable);
        return null;
    }
}

