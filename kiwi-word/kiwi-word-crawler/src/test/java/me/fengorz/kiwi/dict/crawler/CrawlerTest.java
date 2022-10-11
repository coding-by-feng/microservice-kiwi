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

package me.fengorz.kiwi.dict.crawler;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.constant.EnvConstants;
import me.fengorz.kiwi.common.sdk.util.spring.SpringUtils;
import me.fengorz.kiwi.dict.crawler.common.CrawlerConstants;
import me.fengorz.kiwi.dict.crawler.component.Enabler;
import me.fengorz.kiwi.dict.crawler.component.scheduler.ChiefProducerSchedulerSetup;
import me.fengorz.kiwi.dict.crawler.component.scheduler.ChiefSchedulerSetup;
import me.fengorz.kiwi.dict.crawler.component.scheduler.base.DailyScheduler;
import me.fengorz.kiwi.dict.crawler.component.scheduler.base.Scheduler;
import me.fengorz.kiwi.dict.crawler.config.properties.CrawlerConfigProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;

@Slf4j
@ActiveProfiles({EnvConstants.DEV})
@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:env.properties")
@SpringBootTest(classes = WordCrawlerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CrawlerTest {

    @Autowired
    private CrawlerConfigProperties crawlerConfigProperties;
    @Autowired
    private Enabler enabler;
    @Resource(name = CrawlerConstants.COMPONENT_BEAN_ID.CACHE_WORD_SCHEDULER)
    private Scheduler cacheWordScheduler;
    @Resource(name = CrawlerConstants.COMPONENT_BEAN_ID.GENERATE_VOICE_ONLY_COLLECTED_SCHEDULER)
    private Scheduler generateVoiceOnlyCollectedScheduler;
    @Autowired
    private ChiefSchedulerSetup chiefSchedulerSetup;
    @Autowired
    private ChiefProducerSchedulerSetup chiefProducerSchedulerSetup;

    @Test
    @Disabled
    public void setupTest() {
        Assertions.assertNotNull(crawlerConfigProperties);
        Assertions.assertNotNull(crawlerConfigProperties.getEnableScheduler());
        Assertions.assertTrue(enabler.isMqEnable());
        Assertions.assertTrue(crawlerConfigProperties.getEnableScheduler()
            .get(CrawlerConstants.ENABLE_SCHEDULER_KEY.VOICE_GENERATE_ONLY_COLLECTED));
        Assertions.assertFalse(crawlerConfigProperties.getEnableScheduler()
            .get(CrawlerConstants.ENABLE_SCHEDULER_KEY.VOICE_GENERATE_NON_COLLECTED));
        log.info("enabler.isMqEnable() is {}", enabler.isMqEnable());
        log.info("crawlerConfigProperties.getSchedulerEnable() is {}", crawlerConfigProperties.getEnableScheduler());
    }

    @Test
    @Disabled
    public void cacheWordScheduler() {
        cacheWordScheduler.schedule();
    }

    @Test
    @Disabled
    public void test_RefreshAllApiKey() {
        DailyScheduler scheduler =
            SpringUtils.getBean(CrawlerConstants.COMPONENT_BEAN_ID.CACHE_WORD_SCHEDULER, DailyScheduler.class);
        scheduler.schedule();
    }

    @Test
    // @Disabled
    public void schedulerTest() {
        Assertions.assertDoesNotThrow(() -> {
            // Assertions.assertNotNull(chiefSchedulerSetup);
            // Assertions.assertNotNull(chiefProducerSchedulerSetup);
            // chiefSchedulerSetup.setup();
            chiefProducerSchedulerSetup.produce();
            // generateVoiceOnlyCollectedScheduler.schedule();
        });
    }

}