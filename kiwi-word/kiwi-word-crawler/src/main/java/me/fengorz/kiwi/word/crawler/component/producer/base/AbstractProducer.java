/*
 *
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 *
 */

package me.fengorz.kiwi.word.crawler.component.producer.base;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.common.sdk.util.lang.collection.KiwiCollectionUtils;
import me.fengorz.kiwi.word.api.dto.remote.WordFetchQueuePageDTO;
import me.fengorz.kiwi.word.api.entity.WordFetchQueueDO;
import me.fengorz.kiwi.word.api.feign.IWordFetchAPI;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2020/7/29 2:16 PM
 */
@RequiredArgsConstructor
public abstract class AbstractProducer {

    protected final IWordFetchAPI wordFetchAPI;
    protected final ISender sender;

    protected List<WordFetchQueueDO> getQueueDO(Integer status) {
        WordFetchQueueDO wordFetchQueue = new WordFetchQueueDO().setFetchStatus(status)
            .setIsValid(CommonConstants.FLAG_Y).setIsLock(CommonConstants.FLAG_NO);
        WordFetchQueuePageDTO wordFetchQueuePage =
            new WordFetchQueuePageDTO().setWordFetchQueue(wordFetchQueue).setPage(new Page<>(1, 20));
        return Optional.of(wordFetchAPI.pageQueue(wordFetchQueuePage)).get().getData();
    }

    protected void produce(Integer status) {
        List<WordFetchQueueDO> list = this.getQueueDO(status);
        if (KiwiCollectionUtils.isEmpty(list)) {
            return;
        }
        list.forEach(this::execute);
    }

    protected abstract void execute(WordFetchQueueDO queue);
}
