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

package me.fengorz.kiwi.common.sdk.util.validate;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import me.fengorz.kiwi.common.sdk.exception.ResourceNotFoundException;
import me.fengorz.kiwi.common.sdk.exception.ServiceException;
import me.fengorz.kiwi.common.sdk.util.lang.string.KiwiStringUtils;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;

/**
 * @Description 断言工具类
 * @Author zhanshifeng
 * @Date 2019/11/26 9:38 PM
 */
public class KiwiAssertUtils extends Assert {

    public static <T> void assertNotNullThrowServiceException(T object, String errorMsgTemplate, Object... params) {
        if (object == null) {
            throw new ServiceException(StrUtil.format(errorMsgTemplate, params));
        }
    }

    public static <T> void resourceNotNull(T object, String errorMsgTemplate, Object... params) {
        if (object == null) {
            throw new ResourceNotFoundException(StrUtil.format(errorMsgTemplate, params));
        }
    }

    public static <T> T resourceNotEmpty(T object, String errorMsgTemplate, Object... params) {
        resourceNotNull(object, errorMsgTemplate, params);
        if (object instanceof String) {
            if (KiwiStringUtils.isBlank(object.toString())) {
                throw new ResourceNotFoundException(StrUtil.format(errorMsgTemplate, params));
            } else {
                return object;
            }
        }
        if (object instanceof Collection) {
            if (CollectionUtils.isEmpty((Collection<?>) object)) {
                throw new ResourceNotFoundException(StrUtil.format(errorMsgTemplate, params));
            } else {
                return object;
            }
        }
        if (object instanceof Integer) {
            if (object.equals(0)) {
                throw new ResourceNotFoundException(StrUtil.format(errorMsgTemplate, params));
            } else {
                return object;
            }
        }
        return object;
    }

    public static <T> T assertNotEmpty(T object, String errorMsgTemplate, Object... params) {
        assertNotNullThrowServiceException(object, errorMsgTemplate, params);
        if (object instanceof String) {
            if (KiwiStringUtils.isBlank(object.toString())) {
                throw new ServiceException(StrUtil.format(errorMsgTemplate, params));
            } else {
                return object;
            }
        }
        if (object instanceof Collection) {
            if (CollectionUtils.isEmpty((Collection<?>) object)) {
                throw new ServiceException(StrUtil.format(errorMsgTemplate, params));
            } else {
                return object;
            }
        }
        if (object instanceof Integer) {
            if (object.equals(0)) {
                throw new ServiceException(StrUtil.format(errorMsgTemplate, params));
            } else {
                return object;
            }
        }
        return object;
    }

    public static void isTrue(boolean expression, String errorMsgTemplate, Object... params)
            throws IllegalArgumentException {
        if (!expression) {
            throw new ServiceException(StrUtil.format(errorMsgTemplate, params));
        }
    }
}
