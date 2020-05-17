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
import me.fengorz.kiwi.word.api.entity.WordPronunciationDO;
import me.fengorz.kiwi.word.api.feign.IRemoteWordPronunciationService;
import org.springframework.stereotype.Component;


/**
 * 单词例句表
 *
 * @author codingByFeng
 * @date 2019-11-01 14:44:45
 */
@Slf4j
@Component
public class RemoteWordPronunciationServiceFallbackImpl implements IRemoteWordPronunciationService {

    @Setter
    private Throwable throwable;

    @Override
    public R getWordPronunciationPage(Page page, WordPronunciationDO wordPronunciation) {
        log.error("getWordPronunciationPage error, wordPronunciation=" + wordPronunciation, throwable);
        return null;
    }

    @Override
    public R getOne(WordPronunciationDO condition) {
        log.error("getOne error, wordPronunciation=" + condition, throwable);
        return null;
    }

    @Override
    public R getById(Integer pronunciationId) {
        log.error("getById error, pronunciationId=" + pronunciationId, throwable);
        return null;
    }

    @Override
    public R save(WordPronunciationDO WordPronunciation) {
        log.error("save error, WordPronunciation=" + WordPronunciation, throwable);
        return null;
    }

    @Override
    public R updateById(WordPronunciationDO wordPronunciation) {
        log.error("updateById error, WordPronunciation=" + wordPronunciation, throwable);
        return null;
    }

    @Override
    public R removeById(Integer pronunciationId) {
        log.error("removeById error, pronunciationId=" + pronunciationId, throwable);
        return null;
    }
}

