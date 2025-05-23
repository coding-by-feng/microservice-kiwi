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

package me.fengorz.kiwi.crawler.config.properties;

import lombok.Data;

/**
 * @Author Kason Zhan @Date 2020/10/15 8:33 PM
 */
@Data
public class MqExchange {

    private String exchange;
    private String fetchQueue;
    private String fetchRouting;
    private String removeQueue;
    private String removeRouting;
}
