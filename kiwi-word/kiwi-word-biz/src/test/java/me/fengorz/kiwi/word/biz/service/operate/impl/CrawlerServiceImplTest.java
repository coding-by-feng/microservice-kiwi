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

package me.fengorz.kiwi.word.biz.service.operate.impl;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import cn.hutool.core.thread.ThreadUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.constant.EnvConstants;
import me.fengorz.kiwi.word.biz.WordBizApplication;
import me.fengorz.kiwi.word.biz.service.operate.CrawlerService;

@Slf4j
@ActiveProfiles({EnvConstants.DEV, EnvConstants.BASE})
@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:env.properties")
@SpringBootTest(classes = WordBizApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CrawlerServiceImplTest {

    @Autowired
    private CrawlerService crawlerService;

    // @Test
    @SneakyThrows
    @RepeatedTest(value = 3)
    void generateTtsVoice() {
        CountDownLatch latch = new CountDownLatch(1);
        ThreadUtil.execAsync(() -> {
            Assertions.assertDoesNotThrow(() -> crawlerService.generateTtsVoice());
            latch.countDown();
        });
        Assertions.assertDoesNotThrow(() -> crawlerService.generateTtsVoice());
        latch.await();
    }
}