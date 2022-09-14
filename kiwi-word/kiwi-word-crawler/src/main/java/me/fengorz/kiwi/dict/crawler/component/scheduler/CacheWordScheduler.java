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

package me.fengorz.kiwi.dict.crawler.component.scheduler;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.annotation.ScheduledAwake;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.util.lang.collection.KiwiCollectionUtils;
import me.fengorz.kiwi.dict.crawler.component.scheduler.base.AbstractFetchScheduler;
import me.fengorz.kiwi.dict.crawler.component.scheduler.base.Scheduler;
import me.fengorz.kiwi.dict.crawler.component.scheduler.base.SchedulerDTO;
import me.fengorz.kiwi.word.api.entity.FetchQueueDO;
import me.fengorz.kiwi.word.api.feign.IBizAPI;
import me.fengorz.kiwi.word.api.feign.IQueryAPI;
import me.fengorz.kiwi.word.api.util.WordApiUtils;

/**
 * 自动将所有未入缓存的单词纳入缓存 @Author zhanshifeng @Date 2020/9/17 6:14 PM
 */
@Slf4j
@Component("cacheWordScheduler")
public class CacheWordScheduler extends AbstractFetchScheduler implements Scheduler {

    private static final String CACHING_WORD = "caching word {}!";
    private final IQueryAPI queryAPI;

    public CacheWordScheduler(final IBizAPI bizAPI, final IQueryAPI queryAPI) {
        super(bizAPI);
        this.queryAPI = queryAPI;
    }

    @Override
    @ScheduledAwake(key = "cache-word")
    public void schedule() {
        log.info("Word cache schedule is beginning.");
        try {
            super.schedule(null);
        } catch (Exception e) {
            log.error("Word cache schedule failed.", e);
        }
    }

    @Override
    public List<FetchQueueDO> getQueueDO(SchedulerDTO dto) {
        List<FetchQueueDO> list = bizAPI.listNotIntoCache().getData();
        if (!KiwiCollectionUtils.isEmpty(list)) {
            countDownLatch = new CountDownLatch(list.size());
        }
        return list;
    }

    /**
     * 将单词扔进缓存
     *
     * @param queue
     */
    @Override
    protected void execute(FetchQueueDO queue) {
        String wordName = queue.getWordName();
        log.info(CACHING_WORD, wordName);
        try {
            queryAPI.queryWord(WordApiUtils.decode(wordName));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            queue.setIsIntoCache(GlobalConstants.FLAG_YES);
            countDownLatch.countDown();
            bizAPI.updateQueueById(queue);
        }
    }
}
