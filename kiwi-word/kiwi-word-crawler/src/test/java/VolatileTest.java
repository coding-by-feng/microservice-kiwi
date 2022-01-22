/*
 *
 * Copyright [2019~2025] [zhanshifeng]
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author zhanshifeng @Date 2020/1/3 9:47 AM
 */
public class VolatileTest {
    private static final int THREADS_COUNT = 100;
    public static volatile int race = 0;
    public static AtomicInteger raceAtomic = new AtomicInteger(0);
    private static CountDownLatch countDownLatch = new CountDownLatch(THREADS_COUNT);

    public static synchronized void increase() {
        // race++;
        raceAtomic.getAndIncrement();
    }

    public static void main(String[] args) throws InterruptedException {
        long start = System.currentTimeMillis();
        Thread[] threads = new Thread[THREADS_COUNT];
        for (int i = 0; i < THREADS_COUNT; i++) {
            threads[i] =
                    new Thread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    for (int i = 0; i < 100000; i++) {
                                        increase();
                                    }
                                    countDownLatch.countDown();
                                }
                            });
            threads[i].start();
        }
        countDownLatch.await();
        long end = System.currentTimeMillis();
        System.out.println(raceAtomic);
        System.out.println((end - start) / 1000D + "s");
    }
}
