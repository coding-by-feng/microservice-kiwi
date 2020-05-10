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
import me.fengorz.kiwi.word.api.entity.WordParaphraseStarListDO;
import me.fengorz.kiwi.word.api.feign.IRemoteWordParaphraseStarListService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


/**
 * 单词本
 *
 * @author codingByFeng
 * @date 2019-12-08 23:27:41
 */
@Slf4j
@Component
public class RemoteWordParaphraseStarListServiceFallbackImpl implements IRemoteWordParaphraseStarListService {

    @Setter
    private Throwable throwable;

    @Override
    public R getWordParaphraseStarListPage(Page page, WordParaphraseStarListDO wordParaphraseStarListDO) {
        log.error("getWordParaphraseStarListPage error, wordParaphraseStarListDO=" + wordParaphraseStarListDO, throwable);
        return null;
    }

    @Override
    public R getOne(WordParaphraseStarListDO condition) {
        log.error("getOne error, wordParaphraseStarListDO=" + condition, throwable);
        return null;
    }

    @Override
    public R getById(Integer id) {
        log.error("getById error, id=" + id, throwable);
        return null;
    }

    @Override
    public R save(WordParaphraseStarListDO WordParaphraseStarListDO) {
        log.error("save error, WordParaphraseStarListDO=" + WordParaphraseStarListDO, throwable);
        return null;
    }

    @Override
    public R updateById(WordParaphraseStarListDO wordParaphraseStarListDO) {
        log.error("updateById error, WordParaphraseStarListDO=" + wordParaphraseStarListDO, throwable);
        return null;
    }

    @Override
    public R removeById(Integer id) {
        log.error("removeById error, id=" + id, throwable);
        return null;
    }
}

