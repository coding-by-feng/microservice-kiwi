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

package me.fengorz.kiwi.common.sdk.util.lang.collection;

import cn.hutool.core.collection.CollectionUtil;

import java.util.Map;

/**
 * @Description 集合工具类
 * @Author ZhanShiFeng
 * @Date 2020/5/17 9:31 AM
 */
public class KiwiCollectionUtils extends CollectionUtil {

    public static <K, V> Map<K, V> putAndReturn(Map<K, V> map, K k, V v) {
        map.put(k, v);
        return map;
    }

}
