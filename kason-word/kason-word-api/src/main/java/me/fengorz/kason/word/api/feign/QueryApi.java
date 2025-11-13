/*
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
 */
package me.fengorz.kason.word.api.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.fengorz.kason.common.api.R;
import me.fengorz.kason.common.sdk.constant.EnvConstants;
import me.fengorz.kason.word.api.feign.factory.QueryApiFallbackFactory;
import me.fengorz.kason.word.api.vo.detail.WordQueryVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * @Author Kason Zhan
 * @date 2020-05-24 01:40:36
 */
@FeignClient(contextId = "queryApi", value = EnvConstants.APPLICATION_NAME_KASON_WORD_BIZ,
    fallbackFactory = QueryApiFallbackFactory.class)
public interface QueryApi {

    String WORD_MAIN = "/word/main";
    String WORD_MAIN_VARIANT = "/word/main/variant";

    @GetMapping(WORD_MAIN + "/query/{wordName}")
    R<Page<WordQueryVO>> queryWord(@PathVariable String wordName);

    @PostMapping(WORD_MAIN_VARIANT + "/insertVariant/{inputWordName}/{fetchWordName}")
    R<Void> insertVariant(@PathVariable String inputWordName, @PathVariable String fetchWordName);

}
