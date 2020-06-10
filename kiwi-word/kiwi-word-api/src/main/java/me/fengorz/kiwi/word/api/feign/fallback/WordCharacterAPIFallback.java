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

package me.fengorz.kiwi.word.api.feign.fallback;

import org.springframework.stereotype.Component;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.word.api.feign.IWordCharacterAPI;

/**
 * 单词词性表
 *
 * @author zhanshifeng
 * @date 2019-11-01 14:37:07
 */
@Slf4j
@Component
public class WordCharacterAPIFallback implements IWordCharacterAPI {

    @Setter
    private Throwable throwable;

}
