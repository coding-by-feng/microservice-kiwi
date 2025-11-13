/*
 *
 * Copyright [2019~2025] [codingByFeng]
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

package me.fengorz.kason.common.sdk.exception;

import me.fengorz.kason.common.api.ResultCode;

/**
 * @Description 操作资源不存在异常 @Author Kason Zhan @Date 2020/5/24 12:38 PM
 */
public class ResourceNotFoundException extends BaseRuntimeException {

    public ResourceNotFoundException() {
        super();
    }

    public ResourceNotFoundException(Throwable cause) {
        super(cause);
    }

    public ResourceNotFoundException(String msg, Object... args) {
        super(msg, args);
    }

    public ResourceNotFoundException(String msg, Throwable throwable, Object... args) {
        super(msg, throwable, args);
    }

    public ResourceNotFoundException(ResultCode resultCode, Object... args) {
        super(resultCode, args);
    }

    public ResourceNotFoundException(String msg, ResultCode resultCode, Object... args) {
        super(msg, resultCode, args);
    }

    public ResourceNotFoundException(String msg, ResultCode resultCode, Throwable throwable, Object... args) {
        super(msg, resultCode, throwable, args);
    }
}
