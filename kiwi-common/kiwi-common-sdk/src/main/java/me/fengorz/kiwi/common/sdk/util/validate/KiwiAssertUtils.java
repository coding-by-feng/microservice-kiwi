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

package me.fengorz.kiwi.common.sdk.util.validate;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import me.fengorz.kiwi.common.api.exception.ResourceNotFoundException;
import me.fengorz.kiwi.common.api.exception.ServiceException;
import me.fengorz.kiwi.common.sdk.util.lang.string.KiwiStringUtils;

import java.util.Collection;

/**
 * @Description 断言工具类
 * @Author ZhanShiFeng
 * @Date 2019/11/26 9:38 PM
 */
public class KiwiAssertUtils extends Assert {

    public static <T> T serviceNotNull(T object, String errorMsgTemplate, Object... params) {
        if (object == null) {
            throw new ServiceException(StrUtil.format(errorMsgTemplate, params));
        }
        return object;
    }

    public static <T> T resourceNotNull(T object, String errorMsgTemplate, Object... params){
        if (object == null) {
            throw new ResourceNotFoundException(StrUtil.format(errorMsgTemplate, params));
        }
        return object;
    }

    public static <T> T serviceEmpty(T object, String errorMsgTemplate, Object... params) {
        serviceNotNull(object, errorMsgTemplate, params);
        if (object instanceof String && KiwiStringUtils.isBlank(object.toString())) {
            throw new ServiceException(StrUtil.format(errorMsgTemplate, params));
        }

        if (object instanceof Collection && !((Collection) object).isEmpty()) {
            throw new ServiceException(StrUtil.format(errorMsgTemplate, params));
        }
        if ((object instanceof Integer) && !object.equals(0)) {
            throw new ServiceException(StrUtil.format(errorMsgTemplate, params));
        }
        return object;
    }


    public static <T> T serviceNotEmpty(T object, String errorMsgTemplate, Object... params) {
        serviceNotNull(object, errorMsgTemplate, params);
        if (object instanceof Collection && ((Collection) object).isEmpty()) {
            throw new ServiceException(StrUtil.format(errorMsgTemplate, params));
        }
        if ((object instanceof Integer) && ((Integer) object).equals(0)) {
            throw new ServiceException(StrUtil.format(errorMsgTemplate, params));
        }
        return object;
    }


}
