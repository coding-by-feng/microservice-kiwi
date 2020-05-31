/*
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.fengorz.kiwi.word.api.feign;

import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.word.api.common.WordConstants;
import me.fengorz.kiwi.word.api.vo.WordMainVariantVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


/**
 * @Author zhanshifeng
 * @date 2020-05-24 01:40:36
 */
@FeignClient(contextId = "remoteWordMainVariantService", value = WordConstants.KIWI_WORD_BIZ)
public interface IWordMainVariantAPIService {

    String WORD_MAIN_VARIANT = "/word/main/variant";

    @GetMapping(WORD_MAIN_VARIANT + "/get/{id}")
    R<WordMainVariantVO> get(@PathVariable Integer id);

    @PostMapping(WORD_MAIN_VARIANT + "/insertVariant")
    R<Void> insertVariant(@RequestParam String inputWordName, @RequestParam String fetchWordName);

}

