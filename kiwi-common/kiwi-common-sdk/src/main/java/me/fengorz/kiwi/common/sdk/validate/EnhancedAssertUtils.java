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

package me.fengorz.kiwi.common.sdk.validate;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import me.fengorz.kiwi.common.api.exception.ServiceException;

import java.util.Collection;

/**
 * @Description 断言工具类
 * @Author zhanshifeng
 * @Date 2019/11/26 9:38 PM
 */
public class EnhancedAssertUtils extends Assert {

    public static <T> T serviceNotNull(T object, String errorMsgTemplate, Object... params) throws ServiceException {
        if (object == null) {
            throw new ServiceException(StrUtil.format(errorMsgTemplate, params));
        }
        return object;
    }

    public static <T> T serviceEmpty(T object, String errorMsgTemplate, Object... params) throws ServiceException {
        serviceNotNull(object, errorMsgTemplate, params);
        if (object instanceof Collection && !((Collection) object).isEmpty()) {
            throw new ServiceException(StrUtil.format(errorMsgTemplate, params));
        }
        if ((object instanceof Integer) && !object.equals(0)) {
            throw new ServiceException(StrUtil.format(errorMsgTemplate, params));
        }
        return object;
    }


    public static <T> T serviceNotEmpty(T object, String errorMsgTemplate, Object... params) throws ServiceException {
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
