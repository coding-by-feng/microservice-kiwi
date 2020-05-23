/*
 *
 *   Copyright [2019~2025] [codingByFeng]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *
 */

package me.fengorz.kiwi.bdf.cache.redis;

import lombok.NoArgsConstructor;
import me.fengorz.kiwi.common.api.annotation.cache.KiwiCacheKey;
import me.fengorz.kiwi.common.api.annotation.cache.KiwiCacheKeyPrefix;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.common.sdk.util.lang.array.KiwiArrayUtils;
import me.fengorz.kiwi.common.sdk.util.lang.object.KiwiObjectUtils;
import me.fengorz.kiwi.common.sdk.util.lang.string.KiwiStringUtils;
import org.springframework.cache.interceptor.SimpleKeyGenerator;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Description 自定义redis key的生成器
 * @Author zhanshifeng
 * @Date 2020/5/17 11:20 AM
 */
@NoArgsConstructor
public class CacheKeyGenerator extends SimpleKeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        String prefix;

        KiwiCacheKeyPrefix kiwiCacheKeyPrefix = Optional.ofNullable(
                method.getAnnotation(KiwiCacheKeyPrefix.class)
        ).orElse(target.getClass().getAnnotation(KiwiCacheKeyPrefix.class));
        prefix = kiwiCacheKeyPrefix == null ? CommonConstants.EMPTY : kiwiCacheKeyPrefix.value() + CommonConstants.SYMBOL_DELIMITER_STR;

        Parameter[] parameters = method.getParameters();
        if (KiwiArrayUtils.isNotEmpty(parameters)) {
            AtomicInteger paramsIndex = new AtomicInteger();
            SortedMap<Integer, Object> sortedMap = new TreeMap<>();
            Arrays.stream(parameters).peek(parameter -> {
                Object param = params[paramsIndex.get()];
                Optional.ofNullable(parameter.getAnnotation(KiwiCacheKey.class))
                        .ifPresent(kiwiCacheKey -> {
                            if (kiwiCacheKey.nullable() || KiwiObjectUtils.isNotEmpty(param)) {
                                sortedMap.put(kiwiCacheKey.value(), param);
                            }
                        });
                paramsIndex.getAndIncrement();
            });

            if (!sortedMap.isEmpty()) {
                return prefix.concat(KiwiStringUtils.join(CommonConstants.SYMBOL_DELIMITER_STR, sortedMap.values()));
            }
        }

        return prefix.concat(super.generate(target, method, params).toString());
    }
}