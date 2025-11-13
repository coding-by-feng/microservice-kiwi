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

package me.fengorz.kason.crawler;

import me.fengorz.kason.bdf.feign.annotation.EnableEnhancerFeignClients;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;

/**
 * @Author Kason Zhan
 * @Date 2019/10/29 9:27 AM
 */
@SpringCloudApplication
@EnableEnhancerFeignClients
public class WordCrawlerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WordCrawlerApplication.class, args);
    }

}
