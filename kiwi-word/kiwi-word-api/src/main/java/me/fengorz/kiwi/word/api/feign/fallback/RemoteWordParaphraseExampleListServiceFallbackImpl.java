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
import me.fengorz.kiwi.word.api.entity.WordExampleStarListDO;
import me.fengorz.kiwi.word.api.feign.IRemoteWordParaphraseExampleListService;
import org.springframework.stereotype.Component;


/**
 * @author codingByFeng
 * @date 2019-12-08 23:27:12
 */
@Slf4j
@Component
public class RemoteWordParaphraseExampleListServiceFallbackImpl implements IRemoteWordParaphraseExampleListService {

    @Setter
    private Throwable throwable;

    @Override
    public R getWordParaphraseExampleListPage(Page page, WordExampleStarListDO wordExampleStarListDO) {
        log.error("getWordParaphraseExampleListPage error, wordExampleStarListDO=" + wordExampleStarListDO, throwable);
        return null;
    }

    @Override
    public R getOne(WordExampleStarListDO condition) {
        log.error("getOne error, wordParaphraseExampleListDO=" + condition, throwable);
        return null;
    }

    @Override
    public R getById(Integer id) {
        log.error("getById error, id=" + id, throwable);
        return null;
    }

    @Override
    public R save(WordExampleStarListDO WordExampleStarListDO) {
        log.error("save error, WordExampleStarListDO=" + WordExampleStarListDO, throwable);
        return null;
    }

    @Override
    public R updateById(WordExampleStarListDO wordExampleStarListDO) {
        log.error("updateById error, WordExampleStarListDO=" + wordExampleStarListDO, throwable);
        return null;
    }

    @Override
    public R removeById(Integer id) {
        log.error("removeById error, id=" + id, throwable);
        return null;
    }
}

