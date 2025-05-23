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

package me.fengorz.kiwi.common.sdk.exception;

import lombok.NoArgsConstructor;

/**
 * @Author Kason Zhan
 * @Date 2019-09-10 14:48
 */
@NoArgsConstructor
public class DataCheckedException extends BaseException {

    private static final long serialVersionUID = 4231666829536890844L;

    public DataCheckedException(String message) {
        super(message);
    }

    public DataCheckedException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataCheckedException(Throwable cause) {
        super(cause);
    }

    public DataCheckedException(String msg, Object... args) {
        super(msg, args);
    }
}
