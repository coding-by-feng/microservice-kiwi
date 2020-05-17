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
import me.fengorz.kiwi.word.api.entity.WordStarListDO;
import me.fengorz.kiwi.word.api.feign.IRemoteWordStarListService;
import org.springframework.stereotype.Component;


/**
 * 单词本
 *
 * @author codingByFeng
 * @date 2019-12-08 23:26:57
 */
@Slf4j
@Component
public class RemoteWordStarListServiceFallbackImpl implements IRemoteWordStarListService {

    @Setter
    private Throwable throwable;

    @Override
    public R getWordStarListPage(Page page, WordStarListDO wordStarListDO) {
        log.error("getWordStarListPage error, wordStarListDO=" + wordStarListDO, throwable);
        return null;
    }

    @Override
    public R getOne(WordStarListDO condition) {
        log.error("getOne error, wordStarListDO=" + condition, throwable);
        return null;
    }

    @Override
    public R getById(Integer id) {
        log.error("getById error, id=" + id, throwable);
        return null;
    }

    @Override
    public R save(WordStarListDO WordStarListDO) {
        log.error("save error, WordStarListDO=" + WordStarListDO, throwable);
        return null;
    }

    @Override
    public R updateById(WordStarListDO wordStarListDO) {
        log.error("updateById error, WordStarListDO=" + wordStarListDO, throwable);
        return null;
    }

    @Override
    public R removeById(Integer id) {
        log.error("removeById error, id=" + id, throwable);
        return null;
    }
}

