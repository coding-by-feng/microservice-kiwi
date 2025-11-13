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

package me.fengorz.kason.common.cache.redis;

import lombok.NoArgsConstructor;
import me.fengorz.kason.common.sdk.annotation.cache.KasonCacheKey;
import me.fengorz.kason.common.sdk.annotation.cache.KasonCacheKeyPrefix;
import me.fengorz.kason.common.sdk.constant.GlobalConstants;
import me.fengorz.kason.common.sdk.util.lang.array.KasonArrayUtils;
import me.fengorz.kason.common.sdk.util.lang.object.KasonObjectUtils;
import me.fengorz.kason.common.sdk.util.lang.string.KasonStringUtils;
import me.fengorz.kason.common.sdk.util.validate.KasonAssertUtils;
import org.springframework.cache.interceptor.SimpleKeyGenerator;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @Description 自定义redis key的生成器 @Author Kason Zhan @Date 2020/5/17 11:20 AM
 */
@NoArgsConstructor
public class CacheKeyGenerator extends SimpleKeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        String prefix = GlobalConstants.EMPTY;

        KasonCacheKeyPrefix classKeyPrefix = target.getClass().getAnnotation(KasonCacheKeyPrefix.class);
        if (Objects.nonNull(classKeyPrefix)) {
            prefix = classKeyPrefix.value() + GlobalConstants.SYMBOL_DELIMITER_STR;
        }

        KasonCacheKeyPrefix methodKeyPrefix = method.getAnnotation(KasonCacheKeyPrefix.class);
        if (Objects.nonNull(methodKeyPrefix)) {
            prefix += methodKeyPrefix.value() + GlobalConstants.SYMBOL_DELIMITER_STR;
        }

        KasonAssertUtils.assertNotEmpty(prefix, "Class[{}], Method[{}]: CacheKeyPrefix cannot be null!", classKeyPrefix,
            methodKeyPrefix);

        Parameter[] parameters = method.getParameters();
        if (KasonArrayUtils.isNotEmpty(parameters)) {
            SortedMap<Integer, Object> sortedMap = new TreeMap<>();
            int i = -1;
            for (Parameter parameter : parameters) {
                i++;
                if (i >= params.length || parameter == null) {
                    continue;
                }
                Object param = params[i];
                Optional.ofNullable(parameter.getAnnotation(KasonCacheKey.class)).ifPresent(kasonCacheKey -> {
                    if (kasonCacheKey.nullable() || KasonObjectUtils.isNotEmpty(param)) {
                        sortedMap.put(kasonCacheKey.value(), param);
                    }
                });
            }

            if (!sortedMap.isEmpty()) {
                return prefix.concat(KasonStringUtils.join(GlobalConstants.SYMBOL_DELIMITER_STR, sortedMap.values()));
            }
        }

        return prefix.concat(super.generate(target, method, params).toString());
    }
}
