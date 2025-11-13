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

package me.fengorz.kason.dict.crawler;

import me.fengorz.kason.common.sdk.util.json.KasonJsonUtils;
import me.fengorz.kason.crawler.service.JsoupService;
import me.fengorz.kason.crawler.service.impl.JsoupServiceImpl;
import me.fengorz.kason.word.api.dto.queue.FetchPhraseRunUpMqDTO;
import me.fengorz.kason.word.api.dto.queue.result.FetchPhraseRunUpResultDTO;
import me.fengorz.kason.word.api.exception.JsoupFetchConnectException;
import me.fengorz.kason.word.api.exception.JsoupFetchPronunciationException;
import me.fengorz.kason.word.api.exception.JsoupFetchResultException;

/**
 * @Author Kason Zhan @Date 2019/11/4 3:01 PM
 */
public class FetchTest {

    // @Test
    public void test() throws JsoupFetchResultException, JsoupFetchConnectException, JsoupFetchPronunciationException {
        JsoupService jsoupService = new JsoupServiceImpl();
        // FetchWordResultDTO test = jsoupService.fetchWordInfo(new
        // FetchWordMqDTO().setWord("mandatory"));
        FetchPhraseRunUpResultDTO test = jsoupService.fetchPhraseRunUp(new FetchPhraseRunUpMqDTO().setWord("start"));
        System.out.println(KasonJsonUtils.toJsonStr(test));
    }

    // @Test
    public void testSuper() {
        // System.out.println(consumer.getSuperObj());
        // System.out.println(consumer.getThisObj());
        // System.out.println(pronunciationConsumer.getSuperObj());
        // System.out.println(pronunciationConsumer.getThisObj());
    }
}
