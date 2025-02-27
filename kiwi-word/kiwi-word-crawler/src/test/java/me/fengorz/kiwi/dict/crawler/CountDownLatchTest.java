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

import me.fengorz.kiwi.dict.crawler.component.MqMQSender;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author Kason Zhan @Date 2019/10/29 3:19 PM
 */
// @RunWith(SpringRunner.class)
// @SpringBootTest(classes = {RabbitMQConfig.class, QueueConfig.class})
public class CountDownLatchTest implements Runnable {

    private static final CountDownLatch latch = new CountDownLatch(10);
    private static final CountDownLatchTest test = new CountDownLatchTest();
    private Long id;
    // @Autowired
    private MqMQSender mqSender;

    // @PostConstruct
    public void init() {
        this.id = 1L;
    }

    @Override
    public void run() {
        // FetchWordMqDTO wordMessage = new FetchWordMqDTO("test");
        // System.out.println("mq sending a message is " + wordMessage);
        // this.mqSender.fetchWord(wordMessage);
        // latch.countDown();
    }

    public void test() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        // for (int i = 0; i < 1000; i++) executorService.submit(this::test);

        latch.await();

        System.out.println("all sending complete!");

        executorService.shutdown();
    }
}
