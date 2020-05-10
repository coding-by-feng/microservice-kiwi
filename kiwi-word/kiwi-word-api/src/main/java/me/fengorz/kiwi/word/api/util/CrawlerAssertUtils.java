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

package me.fengorz.kiwi.word.api.util;

import cn.hutool.core.util.StrUtil;
import me.fengorz.kiwi.word.api.exception.JsoupFetchResultException;

import java.util.Collection;

/**
 * @Description TODO
 * @Author codingByFeng
 * @Date 2019/10/31 3:58 PM
 */
public class CrawlerAssertUtils {

    public static String fetchValueNotEmpty(String fetchValue, String errorMsgTemplate, Object... params) throws JsoupFetchResultException {
        if (StrUtil.isBlank(fetchValue)) {
            throw new JsoupFetchResultException(StrUtil.format(errorMsgTemplate, params));
        }
        return fetchValue;
    }


    public static Collection notEmpty(Collection collection, String errorMsgTemplate, Object... params) throws JsoupFetchResultException {
        if (collection == null || collection.isEmpty()) {
            throw new JsoupFetchResultException(StrUtil.format(errorMsgTemplate, params));
        }
        return collection;
    }


    public static void mustBeTrue(boolean flag, String errorMsgTemplate, Object... params) throws JsoupFetchResultException {
        if (!flag) {
            throw new JsoupFetchResultException(StrUtil.format(errorMsgTemplate, params));
        }
    }


}
