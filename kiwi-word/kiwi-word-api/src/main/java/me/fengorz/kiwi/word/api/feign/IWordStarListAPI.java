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
package me.fengorz.kiwi.word.api.feign;

import org.springframework.cloud.openfeign.FeignClient;

import me.fengorz.kiwi.word.api.common.WordConstants;
import me.fengorz.kiwi.word.api.feign.factory.WordStarListServiceFallbackFactory;

/**
 * 单词本
 *
 * @author zhanshifeng
 * @date 2019-12-08 23:26:57
 */
@FeignClient(contextId = "remoteWordStarListDOService", value = WordConstants.KIWI_WORD_BIZ,
    fallbackFactory = WordStarListServiceFallbackFactory.class)
public interface IWordStarListAPI {

    String WORD_STAR_LIST = "/word/star/list";

}
