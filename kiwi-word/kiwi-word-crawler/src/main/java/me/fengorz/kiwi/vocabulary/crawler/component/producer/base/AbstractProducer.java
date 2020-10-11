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

package me.fengorz.kiwi.vocabulary.crawler.component.producer.base;

import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.common.sdk.util.lang.collection.KiwiCollectionUtils;
import me.fengorz.kiwi.word.api.entity.FetchQueueDO;
import me.fengorz.kiwi.word.api.feign.IPhraseBizAPI;
import me.fengorz.kiwi.word.api.feign.IWordBizAPI;

import java.util.LinkedList;
import java.util.List;

/**
 * @Author zhanshifeng
 * @Date 2020/7/29 2:16 PM
 */
@RequiredArgsConstructor
public abstract class AbstractProducer implements IProducer {

    protected final IWordBizAPI wordBizAPI;
    protected final IPhraseBizAPI phraseBizAPI;
    protected final ISender sender;
    protected final Object barrier = new Object();
    protected final Integer infoType;

    protected List<FetchQueueDO> getQueueDO(Integer status) {
        synchronized (barrier) {
            return wordBizAPI.pageQueueLockIn(status, 0, 20, infoType).getData();
        }
    }

    protected void produce(Integer... status) {
        List<FetchQueueDO> list = new LinkedList<>();
        for (Integer temp : status) {
            list.addAll(this.getQueueDO(temp));
        }
        if (KiwiCollectionUtils.isEmpty(list)) {
            return;
        }

        // 列表里面每一批查到数据处理完之前先上锁
        synchronized (barrier) {
            list.forEach(this::execute);
        }
    }

    protected abstract void execute(FetchQueueDO queue);
}
