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

package me.fengorz.kiwi.word.crawler.component;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.word.api.dto.fetch.WordMessageDTO;
import me.fengorz.kiwi.word.crawler.service.IWordFetchService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * @Description 采用异步机制去抓取数据和存在数据，这样子可以提交爬虫的效率
 * @Author zhanshifeng
 * @Date 2019/11/27 8:21 PM
 */
@Component
@Slf4j
@AllArgsConstructor
public class AsyncConcurrentConsumer {

    private final IWordFetchService wordFetchService;

    @Async
    @Deprecated
    public void asyncFetchWord(WordMessageDTO wordMessageDTO){
        wordFetchService.work(wordMessageDTO);
    }

}
