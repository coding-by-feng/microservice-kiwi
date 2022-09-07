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

import org.springframework.boot.SpringApplication;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.dict.crawler.component.MqMQSender;
import me.fengorz.kiwi.dict.crawler.component.scheduler.ChiefProducerSchedulerSetup;

/**
 * @Author zhanshifeng @Date 2019/10/28 10:21 AM
 */
// @RunWith(SpringRunner.class)
// @SpringBootTest(classes = {RabbitMQConfig.class, QueueConfig.class})
@Slf4j
public class QueueTest {

    // @Autowired
    private MqMQSender mqSender;

    // @Autowired
    private ChiefProducerSchedulerSetup chiefProducerSchedulerSetup;

    public void main() {
        SpringApplication.run(QueueTest.class);
    }

    // @Test
    // @Transactional
    public void test() {
        // Long id = 1L;
        // while (id < 100L) {
        // Thread.sleep(1000);
        // log.info("word.fetch sending id = " + id);
        // this.wordFetchProducer.send(new WordMessage(id++, "test"));
        // }
        // this.scheduledChiefProducer.fetchWord();
        System.out.println("testing");
    }
}
