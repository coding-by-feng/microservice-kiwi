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

package me.fengorz.kiwi.word.api.feign.fallback;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.api.feign.AbstractFallback;
import me.fengorz.kiwi.word.api.feign.QueryApi;
import me.fengorz.kiwi.word.api.vo.detail.WordQueryVO;
import org.springframework.stereotype.Component;

/**
 * Hystrix熔断回调实现 @Author Kason Zhan
 *
 * @date 2020-05-24 01:40:36
 */
@Slf4j
@Component
public class QueryApiFallback extends AbstractFallback implements QueryApi {

    @Override
    public R<Page<WordQueryVO>> queryWord(String wordName) {
        return handleError();
    }

    @Override
    public R<Void> insertVariant(String inputWordName, String fetchWordName) {
        return handleError();
    }
}
