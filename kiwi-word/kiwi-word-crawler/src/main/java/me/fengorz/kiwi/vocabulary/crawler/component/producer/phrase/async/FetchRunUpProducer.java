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

package me.fengorz.kiwi.vocabulary.crawler.component.producer.phrase.async;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.util.lang.string.KiwiStringUtils;
import me.fengorz.kiwi.vocabulary.crawler.component.producer.base.AbstractProducer;
import me.fengorz.kiwi.vocabulary.crawler.component.producer.base.IProducer;
import me.fengorz.kiwi.vocabulary.crawler.component.producer.base.ISender;
import me.fengorz.kiwi.word.api.common.WordCrawlerConstants;
import me.fengorz.kiwi.word.api.dto.queue.FetchPhraseRunUpMqDTO;
import me.fengorz.kiwi.word.api.entity.FetchQueueDO;
import me.fengorz.kiwi.word.api.feign.IBizAPI;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 抓取词组基本信息-消息队列生产者
 *
 * @Author zhanshifeng
 * @Date 2019/10/30 10:33 AM
 */
@Component
@Slf4j
public class FetchRunUpProducer extends AbstractProducer implements IProducer {

    public FetchRunUpProducer(IBizAPI bizAPI, ISender sender) {
        super(bizAPI, sender);
        this.infoType = WordCrawlerConstants.QUEUE_INFO_TYPE_WORD;
    }

    @Override
    protected List<FetchQueueDO> getQueueDO(Integer status) {
        synchronized (barrier) {
            return bizAPI.pageQueue(status, 0, 20, infoType).getData();
        }
    }

    @Override
    public void produce() {
        super.produce(WordCrawlerConstants.STATUS_ALL_SUCCESS);
    }

    @Async
    @Override
    protected void execute(FetchQueueDO queue) {
        sender.fetchPhraseRunUp(new FetchPhraseRunUpMqDTO().setQueueId(queue.getQueueId()).setWord(KiwiStringUtils.isBlank(queue.getDerivation()) ? queue.getWordName() : queue.getDerivation()).setWordId(queue.getWordId()));
        queue.setFetchStatus(WordCrawlerConstants.STATUS_TO_FETCH_PHRASE);
        bizAPI.updateQueueById(queue);
    }
}
